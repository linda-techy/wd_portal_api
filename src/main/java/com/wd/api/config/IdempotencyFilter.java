package com.wd.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency filter for BOQ financial write endpoints.
 *
 * If a request includes an {@code X-Idempotency-Key} header and a cached result
 * for that key+user combination already exists (within the 5-minute TTL), the
 * cached response is replayed without re-executing the operation.
 *
 * Applied to: PATCH /api/boq/{id}/execute and PATCH /api/boq/{id}/bill
 *
 * Note: This is an in-memory cache — keys are lost on restart. For multi-instance
 * deployments replace the ConcurrentHashMap with a Redis-backed cache.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final long TTL_MILLIS = 5 * 60 * 1_000L; // 5 minutes
    private static final String HEADER = "X-Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 128;

    private record CachedResponse(int status, String contentType, byte[] body, Instant storedAt) {}

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return !("PATCH".equals(method) && (uri.contains("/execute") || uri.contains("/bill")));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(HEADER);

        // No key supplied — pass through normally.
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"X-Idempotency-Key must not exceed 128 characters\"}");
            return;
        }

        String compositeKey = resolveUserPrincipal() + ":" + idempotencyKey;

        // Evict expired entries lazily on each access.
        evictExpired();

        CachedResponse cached = cache.get(compositeKey);
        if (cached != null) {
            logger.debug("Idempotency cache hit for key '{}'", compositeKey);
            response.setStatus(cached.status());
            if (cached.contentType() != null) {
                response.setContentType(cached.contentType());
            }
            response.addHeader("X-Idempotency-Replayed", "true");
            response.getOutputStream().write(cached.body());
            return;
        }

        // Execute the real request, capturing the response body so we can cache it.
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);
        responseWrapper.copyBodyToResponse();

        // Only cache 2xx responses — don't cache errors or validation failures.
        int status = responseWrapper.getStatus();
        if (status >= 200 && status < 300) {
            byte[] body = responseWrapper.getContentAsByteArray();
            cache.put(compositeKey, new CachedResponse(
                    status,
                    responseWrapper.getContentType(),
                    body,
                    Instant.now()));
            logger.debug("Idempotency response cached for key '{}'", compositeKey);
        }
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minusMillis(TTL_MILLIS);
        cache.entrySet().removeIf(e -> e.getValue().storedAt().isBefore(cutoff));
    }

    private String resolveUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
}
