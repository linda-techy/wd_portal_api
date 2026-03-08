package com.wd.api.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * AOP performance monitoring for the Portal API.
 * Logs to performance.log if execution > 1000ms.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger PERF_LOG = LoggerFactory.getLogger(LoggingConstants.PERFORMANCE_LOGGER);

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object monitorControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureAndLog(joinPoint);
    }

    @Around("within(@org.springframework.stereotype.Service *) && !within(com.wd.api.logging..*)")
    public Object monitorServices(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        if (duration >= LoggingConstants.SLOW_SERVICE_THRESHOLD_MS) {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            PERF_LOG.warn("{} | SERVICE | {}.{} | {}ms | traceId={} | userId={}",
                    LoggingConstants.PREFIX_SLOW_API,
                    joinPoint.getTarget().getClass().getSimpleName(), sig.getName(),
                    duration, getTraceId(), getUserId());
        }
        return result;
    }

    private Object measureAndLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        boolean exception = false;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            exception = true;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration >= LoggingConstants.SLOW_API_THRESHOLD_MS) {
                MethodSignature sig = (MethodSignature) joinPoint.getSignature();
                PERF_LOG.warn("{} | CONTROLLER | {} | {}.{} | {}ms | exception={} | traceId={} | userId={}",
                        LoggingConstants.PREFIX_SLOW_API,
                        MDC.get(LoggingConstants.MDC_PATH),
                        joinPoint.getTarget().getClass().getSimpleName(), sig.getName(),
                        duration, exception, getTraceId(), getUserId());
            }
        }
    }

    private String getTraceId() {
        String v = MDC.get(LoggingConstants.MDC_TRACE_ID); return v != null ? v : "NO-TRACE";
    }
    private String getUserId() {
        String v = MDC.get(LoggingConstants.MDC_USER_ID); return v != null ? v : "-";
    }
}
