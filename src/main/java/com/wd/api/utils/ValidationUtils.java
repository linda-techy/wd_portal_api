package com.wd.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization
 */
public class ValidationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // SQL injection prevention patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|onerror|onload)"
    );
    
    // XSS prevention - dangerous characters
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "[<>\"']"
    );
    
    /**
     * Validate and sanitize email address
     */
    public static String validateAndSanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        String sanitized = email.trim().toLowerCase();
        
        if (!EMAIL_PATTERN.matcher(sanitized).matches()) {
            logger.warn("Invalid email format: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize string input to prevent SQL injection and XSS
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            logger.warn("Potential SQL injection attempt detected: {}", sanitized);
            throw new IllegalArgumentException("Invalid input detected");
        }
        
        // Remove XSS dangerous characters (but allow for legitimate use cases)
        // This is a basic check - consider using OWASP Java Encoder for production
        sanitized = sanitized.replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\"", "&quot;")
                            .replace("'", "&#x27;");
        
        return sanitized;
    }
    
    /**
     * Validate string length
     */
    public static void validateLength(String input, int minLength, int maxLength, String fieldName) {
        if (input == null) {
            if (minLength > 0) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return;
        }
        
        int length = input.trim().length();
        
        if (length < minLength) {
            throw new IllegalArgumentException(
                fieldName + " must be at least " + minLength + " characters"
            );
        }
        
        if (length > maxLength) {
            throw new IllegalArgumentException(
                fieldName + " must not exceed " + maxLength + " characters"
            );
        }
    }
    
    /**
     * Validate password strength
     */
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        String pwd = password.trim();
        
        if (pwd.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        if (pwd.length() > 128) {
            throw new IllegalArgumentException("Password must not exceed 128 characters");
        }
        
        // Optional: Add more password strength requirements
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one number
        // - At least one special character
    }
    
    /**
     * Validate numeric range
     */
    public static void validateNumericRange(Number value, Number min, Number max, String fieldName) {
        if (value == null) {
            return; // Null values are handled separately
        }
        
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        
        if (val < minVal || val > maxVal) {
            throw new IllegalArgumentException(
                fieldName + " must be between " + minVal + " and " + maxVal
            );
        }
    }
    
    /**
     * Sanitize for database use (basic)
     */
    public static String sanitizeForDatabase(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes and control characters
        return input.replace("\0", "")
                   .replaceAll("[\u0000-\u001F\u007F-\u009F]", "")
                   .trim();
    }
    
    /**
     * Check if string contains only alphanumeric and allowed special characters
     */
    public static boolean isAlphanumericWithSpecial(String input, String allowedSpecial) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        String pattern = "^[A-Za-z0-9" + Pattern.quote(allowedSpecial) + "]+$";
        return Pattern.matches(pattern, input);
    }
}

