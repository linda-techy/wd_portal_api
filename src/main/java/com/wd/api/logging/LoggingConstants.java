package com.wd.api.logging;

/**
 * Logging constants for the Portal API.
 */
public final class LoggingConstants {

    private LoggingConstants() {}

    public static final String ACCESS_LOGGER      = "ACCESS_LOGGER";
    public static final String SECURITY_LOGGER    = "SECURITY_LOGGER";
    public static final String PERFORMANCE_LOGGER = "PERFORMANCE_LOGGER";

    public static final String MDC_TRACE_ID   = "traceId";
    public static final String MDC_USER_ID    = "userId";
    public static final String MDC_USER_EMAIL = "userEmail";
    public static final String MDC_METHOD     = "httpMethod";
    public static final String MDC_PATH       = "httpPath";

    public static final long SLOW_API_THRESHOLD_MS     = 1000L;
    public static final long SLOW_SERVICE_THRESHOLD_MS = 2000L;

    public static final String[] SENSITIVE_FIELDS = {
        "password", "token", "authorization", "secret",
        "otp", "cvv", "credit_card", "creditcard", "pin"
    };

    public static final String PREFIX_ACCESS   = "ACCESS";
    public static final String PREFIX_SECURITY = "SECURITY";
    public static final String PREFIX_SLOW_API = "SLOW_API";
    public static final String PREFIX_STARTUP  = "STARTUP";
}
