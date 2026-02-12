package com.wd.api.exception;

import com.wd.api.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Security Exceptions
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return buildResponse("You do not have permission to perform this action", HttpStatus.FORBIDDEN);
    }

    // Business Logic Exceptions (Safe to show message)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        logger.warn("Business exception: {}", ex.getMessage());
        ApiError error = new ApiError(ex.getMessage(), ex.getErrorCode());
        ResponseEntity<ApiError> response = ResponseEntity.status(ex.getStatus().value()).body(error);
        return response;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiError apiError = new ApiError("Validation failed");
        apiError.setValidationErrors(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    // No handler found (404) - prevents NoResourceFoundException from being caught by generic handler
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        logger.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getResourcePath());
        return buildResponse("Resource not found: " + ex.getResourcePath(), HttpStatus.NOT_FOUND);
    }

    // Catch-all
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        // "An error occurred" is safer than showing ex.getMessage() for random
        // RuntimeExceptions
        // However, if we trust our code, we might want to log it and show a generic
        // message.
        return buildResponse("An internal error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return buildResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> buildResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status.value()).body(new ApiError(message));
    }
}