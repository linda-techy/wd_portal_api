package com.wd.api.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Intercepts financial write endpoints and enforces per-user rate limits.
 * Applied to: /api/boq/** endpoints matching execute, bill, approve, correct-execution.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int CAPACITY = 10;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    private final RateLimiterConfig rateLimiterConfig;

    public RateLimitInterceptor(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();

        if (!isRateLimitedEndpoint(uri)) {
            return true;
        }

        String key = resolveUserKey(request) + ":" + uri;
        Bucket bucket = rateLimiterConfig.resolveBucket(key, CAPACITY, REFILL_DURATION);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"Rate limit exceeded. Retry after %d seconds.\"}",
            retryAfterSeconds
        ));
        return false;
    }

    private boolean isRateLimitedEndpoint(String uri) {
        return uri.contains("/execute") || uri.contains("/bill")
            || uri.contains("/approve") || uri.contains("/correct-execution");
    }

    private String resolveUserKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return request.getRemoteAddr();
    }
}
