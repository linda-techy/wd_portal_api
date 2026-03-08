package com.wd.api.filter;

import com.wd.api.logging.LoggingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Logs every HTTP request/response to access.log via ACCESS_LOGGER.
 *
 * Example:
 *   ACCESS | GET /api/portal/projects | 200 | 55ms | userId=- | ip=1.2.3.4 | traceId=REQ-abc123
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(LoggingConstants.ACCESS_LOGGER);

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health", "/favicon.ico"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String traceId   = MDC.get(LoggingConstants.MDC_TRACE_ID);
            String userId    = MDC.get(LoggingConstants.MDC_USER_ID);
            String userAgent = request.getHeader("User-Agent");
            String safeUa   = userAgent != null
                    ? userAgent.substring(0, Math.min(userAgent.length(), 80)) : "unknown";

            ACCESS_LOG.info("{} | {} {} | {} | {}ms | userId={} | ip={} | ua={} | traceId={}",
                    LoggingConstants.PREFIX_ACCESS,
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), duration,
                    userId != null ? userId : "-",
                    resolveClientIp(request),
                    safeUa,
                    traceId != null ? traceId : "-");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
