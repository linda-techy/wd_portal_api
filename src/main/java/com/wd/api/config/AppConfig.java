package com.wd.api.config;

import org.springframework.context.annotation.Configuration;

/**
 * Application configuration for the main application.
 * This configuration provides constants and settings for the application.
 */
@Configuration
public class AppConfig {
    
    // Application-specific constants
    public static final String CONTEXT_PATH = "/api";
    public static final String DATABASE_SCHEMA = "public";
    
    // Database table constants
    public static final String USER_TABLE = "portal_users";
    public static final String ROLE_TABLE = "portal_roles";
    public static final String PERMISSION_TABLE = "portal_permissions";
    public static final String REFRESH_TOKEN_TABLE = "portal_refresh_tokens";
} 