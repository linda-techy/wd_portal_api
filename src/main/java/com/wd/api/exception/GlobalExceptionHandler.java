package com.wd.api.exception;

import com.wd.api.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler to catch all unhandled exceptions
 * and provide structured, consistent error responses with correlation IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Get or generate correlation ID for request tracing
     */
    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    // ===== AUTHENTICATION & AUTHORIZATION =====

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Authentication failed: {}", correlationId, ex.getMessage());
        
        ApiError error = new ApiError(
            "Invalid email or password",
            "AUTHENTICATION_FAILED",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Access denied: {}", correlationId, ex.getMessage());
        
        ApiError error = new ApiError(
            "You do not have permission to perform this action",
            "ACCESS_DENIED",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle custom UnauthorizedException (403)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Unauthorized access attempt: {} on {}", 
            correlationId, ex.getAction(), ex.getResource());

        ApiError error = new ApiError(
            "Access denied",  // Generic message for security
            "ACCESS_DENIED",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ===== RESOURCE NOT FOUND =====

    /**
     * Handle custom ResourceNotFoundException (404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Resource not found: {} - {}", 
            correlationId, ex.getResourceType(), ex.getResourceId());

        ApiError error = new ApiError(
            ex.getMessage(),
            "RESOURCE_NOT_FOUND",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] No handler found: {} {}", correlationId, ex.getHttpMethod(), ex.getResourcePath());
        
        ApiError error = new ApiError(
            "Resource not found: " + ex.getResourcePath(),
            "RESOURCE_NOT_FOUND",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ===== BUSINESS LOGIC EXCEPTIONS =====

    /**
     * Handle custom BusinessException (configurable status)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Business exception: {}", correlationId, ex.getMessage());

        ApiError error = new ApiError(
            ex.getMessage(),
            ex.getErrorCode(),
            correlationId,
            request.getRequestURI()
        );
        // ApiException always has a non-null status
        @SuppressWarnings("null")
        var response = ResponseEntity.status(ex.getStatus()).body(error);
        return response;
    }

    // ===== VALIDATION EXCEPTIONS =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Validation failed", correlationId);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", "Input validation failed");
        errorDetails.put("errorCode", "VALIDATION_ERROR");
        errorDetails.put("correlationId", correlationId);
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("validationErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Missing required parameter: {}", correlationId, ex.getParameterName());

        ApiError error = new ApiError(
            "Missing required parameter: " + ex.getParameterName(),
            "MISSING_PARAMETER",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Invalid argument: {}", correlationId, ex.getMessage());

        ApiError error = new ApiError(
            ex.getMessage(),
            "INVALID_ARGUMENT",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ===== HTTP METHOD EXCEPTIONS =====

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.warn("[{}] Method not supported: {}", correlationId, ex.getMethod());

        ApiError error = new ApiError(
            "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
            "METHOD_NOT_ALLOWED",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // ===== FILE/IO EXCEPTIONS =====

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIOException(
            IOException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.error("[{}] IO exception: {}", correlationId, ex.getMessage(), ex);

        ApiError error = new ApiError(
            "File access error occurred",
            "FILE_ACCESS_ERROR",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleFileAccessDeniedException(
            java.nio.file.AccessDeniedException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.error("[{}] File access denied: {}", correlationId, ex.getFile(), ex);

        ApiError error = new ApiError(
            "File permission denied",
            "FILE_PERMISSION_DENIED",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ===== NULL POINTER EXCEPTIONS =====

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiError> handleNullPointerException(
            NullPointerException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.error("[{}] Null pointer exception", correlationId, ex);

        ApiError error = new ApiError(
            "An unexpected error occurred",
            "NULL_POINTER_ERROR",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ===== CATCH-ALL HANDLERS =====

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.error("[{}] Runtime exception: {} - {}", 
            correlationId, ex.getClass().getName(), ex.getMessage(), ex);

        ApiError error = new ApiError(
            "An internal error occurred",
            "INTERNAL_ERROR",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        String correlationId = getCorrelationId();
        logger.error("[{}] Unhandled exception: {} - {}", 
            correlationId, ex.getClass().getName(), ex.getMessage(), ex);

        ApiError error = new ApiError(
            "An unexpected error occurred",
            "INTERNAL_ERROR",
            correlationId,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}