package com.wd.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimiterConfig rateLimiterConfig;

    private final boolean rateLimitingEnabled;

    public WebMvcConfig(RateLimiterConfig rateLimiterConfig,
                        @Value("${app.rate-limiting.enabled:true}") boolean rateLimitingEnabled) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.rateLimitingEnabled = rateLimitingEnabled;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!rateLimitingEnabled) {
            return;
        }

        // BOQ financial write operations — 10 per minute per user
        registry.addInterceptor(new RateLimitInterceptor(rateLimiterConfig))
                .addPathPatterns("/api/boq/**");

        // Auth endpoints — brute force protection per IP
        registry.addInterceptor(new AuthRateLimitInterceptor(rateLimiterConfig))
                .addPathPatterns("/auth/**", "/api/partnerships/**", "/api/customer/**");

        // Public lead submission endpoints — 10 submissions per minute per IP
        registry.addInterceptor(new PublicLeadRateLimitInterceptor(rateLimiterConfig))
                .addPathPatterns("/leads/contact", "/leads/referral");
    }
}
