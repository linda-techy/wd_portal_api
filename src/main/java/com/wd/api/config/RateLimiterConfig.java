package com.wd.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Bucket4j.
 * Protects financial write operations (execute, bill, approve) from abuse.
 *
 * Limit: 10 requests per minute per user/IP on financial endpoints.
 * For distributed deployments, replace the ConcurrentHashMap cache with
 * a Redis-backed Bucket4j ProxyManager.
 */
@Configuration
public class RateLimiterConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Return (or create) a rate-limit bucket for the given key.
     * Each unique key gets its own independent bucket.
     */
    public Bucket resolveBucket(String key, int capacity, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k ->
            Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(capacity)
                    .refillIntervally(capacity, refillDuration)
                    .build())
                .build()
        );
    }
}
