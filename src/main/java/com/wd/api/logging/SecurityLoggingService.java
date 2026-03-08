package com.wd.api.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Logs security events to security.log for the Portal API.
 */
@Service
public class SecurityLoggingService {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger(LoggingConstants.SECURITY_LOGGER);

    public void logLoginSuccess(String email, String ip) {
        SECURITY_LOG.info("{} | LOGIN_SUCCESS | email={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, maskEmail(email), ip, getTraceId());
    }

    public void logLoginFailure(String email, String reason, String ip) {
        SECURITY_LOG.warn("{} | LOGIN_FAILURE | email={} | reason={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, maskEmail(email), reason, ip, getTraceId());
    }

    public void logJwtValidationFailure(String reason, String ip) {
        SECURITY_LOG.warn("{} | JWT_VALIDATION_FAILURE | reason={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, reason, ip, getTraceId());
    }

    public void logUnauthorizedAccess(String path, String method, String userId, String ip) {
        SECURITY_LOG.warn("{} | UNAUTHORIZED_ACCESS | {} {} | userId={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, method, path,
                userId != null ? userId : "anonymous", ip, getTraceId());
    }

    public void logForbiddenAccess(String path, String method, String userId, String ip) {
        SECURITY_LOG.warn("{} | FORBIDDEN_ACCESS | {} {} | userId={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, method, path,
                userId != null ? userId : "anonymous", ip, getTraceId());
    }

    public void logAdminAction(String adminEmail, String action, String target) {
        SECURITY_LOG.info("{} | ADMIN_ACTION | admin={} | action={} | target={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, maskEmail(adminEmail), action, target, getTraceId());
    }

    public void logRateLimitHit(String email, String operation, String ip) {
        SECURITY_LOG.warn("{} | RATE_LIMIT_HIT | email={} | op={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY, maskEmail(email), operation, ip, getTraceId());
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 4) return "***";
        return email.replaceAll("(.).+(@.+)", "$1***$2");
    }

    private String getTraceId() {
        String v = MDC.get(LoggingConstants.MDC_TRACE_ID);
        return v != null ? v : "NO-TRACE";
    }
}
