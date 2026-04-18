package com.wd.api.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * IP-based rate limiter for public lead submission endpoints.
 *
 * Limits: 10 submissions per minute per IP on /leads/contact and /leads/referral.
 * These endpoints are unauthenticated so keying must be by IP rather than user.
 */
public class PublicLeadRateLimitInterceptor implements HandlerInterceptor {

    private static final int CAPACITY = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiterConfig rateLimiterConfig;

    public PublicLeadRateLimitInterceptor(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String ip = resolveClientIp(request);
        String bucketKey = "public-lead:" + request.getRequestURI() + ":" + ip;
        Bucket bucket = rateLimiterConfig.resolveBucket(bucketKey, CAPACITY, WINDOW);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"Too many requests. Retry after %d seconds.\"}",
            retryAfterSeconds
        ));
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
