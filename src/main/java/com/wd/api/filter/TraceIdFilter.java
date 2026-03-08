package com.wd.api.filter;

import com.wd.api.logging.LoggingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceIdFilter — runs first for every portal API request.
 * Generates a unique REQ-{12char} traceId, stores it in MDC
 * so every log line is tagged. Cleans up MDC in finally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        MDC.put(LoggingConstants.MDC_TRACE_ID, traceId);
        MDC.put(LoggingConstants.MDC_METHOD,   request.getMethod());
        MDC.put(LoggingConstants.MDC_PATH,     request.getRequestURI());
        response.setHeader(TRACE_HEADER, traceId);

        try {
            chain.doFilter(request, response);
            populateUserIdMdc();
        } finally {
            MDC.remove(LoggingConstants.MDC_TRACE_ID);
            MDC.remove(LoggingConstants.MDC_USER_ID);
            MDC.remove(LoggingConstants.MDC_USER_EMAIL);
            MDC.remove(LoggingConstants.MDC_METHOD);
            MDC.remove(LoggingConstants.MDC_PATH);
        }
    }

    private void populateUserIdMdc() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(LoggingConstants.MDC_USER_EMAIL, auth.getName());
            }
        } catch (Exception ignored) {}
    }
}
