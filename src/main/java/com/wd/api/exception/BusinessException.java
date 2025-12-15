package com.wd.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception class for business logic errors that are safe to display to the
 * user.
 * e.g., "Lead with this email already exists", "Insufficient budget", etc.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = null;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
