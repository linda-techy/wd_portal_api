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
     * Note: TTL is handled by the scheduled task below (5 minutes).
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("userProjects"),
            new ConcurrentMapCache("userPermissions"),
            new ConcurrentMapCache("projectMetadata"),
            new ConcurrentMapCache("commonData"),
            // Dashboard caches — evicted every 5 min by clearCaches()
            new ConcurrentMapCache("dashboardOverview"),
            new ConcurrentMapCache("dashboardProjects"),
            new ConcurrentMapCache("dashboardLeads"),
            new ConcurrentMapCache("dashboardFinance"),
            new ConcurrentMapCache("dashboardOperations")
        ));
        return cacheManager;
    }

    /**
     * Evict all caches every 5 minutes to prevent out-of-memory errors.
     * This acts as a global TTL for the SimpleCacheManager.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000)
    public void clearCaches() {
        CacheManager manager = cacheManager();
        manager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = manager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}
