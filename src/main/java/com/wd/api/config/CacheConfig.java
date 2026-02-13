package com.wd.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for the portal API.
 * Uses in-memory caching for frequently accessed data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with specific caches.
     * - userProjects: Stores project IDs for each user
     * - userPermissions: User role and permission cache
     * - projectMetadata: Project metadata cache
     * - commonData: Other common data
     * 
     * Note: TTL is handled at the application level (5 minutes).
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        @SuppressWarnings("null")
        var caches = Arrays.asList(
            new ConcurrentMapCache("userProjects"),
            new ConcurrentMapCache("userPermissions"),
            new ConcurrentMapCache("projectMetadata"),
            new ConcurrentMapCache("commonData")
        );
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}
