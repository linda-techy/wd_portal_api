package com.wd.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {
    private boolean success; // Always false for errors
    private String message;
    private String errorCode; // Optional code for frontend logic
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors; // For 400

    public ApiError(String message, String errorCode) {
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(String message) {
        this(message, null);
    }

    // Getters/Setters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
