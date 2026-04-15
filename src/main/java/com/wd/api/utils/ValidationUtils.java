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
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Password complexity pattern: uppercase + lowercase + digit + special char
    private static final Pattern PASSWORD_COMPLEXITY = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$");

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
     * Sanitize string input by trimming and removing control characters.
     * SQL injection is handled by JPA parameterized queries — blocklist patterns
     * cause false positives on legitimate words (e.g. "SELECT spreadsheet").
     * XSS encoding belongs at output time, not input time.
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Trim and remove null bytes and control characters only
        return input.trim()
                .replace("\0", "")
                .replaceAll("[\u0000-\u001F\u007F-\u009F]", "");
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
                    fieldName + " must be at least " + minLength + " characters");
        }

        if (length > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }
    }

    /**
     * Validate password strength.
     * Requires: 12+ chars, uppercase, lowercase, digit, and special character.
     */
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        String pwd = password.trim();

        if (pwd.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }

        if (pwd.length() > 128) {
            throw new IllegalArgumentException("Password must not exceed 128 characters");
        }

        if (!PASSWORD_COMPLEXITY.matcher(pwd).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
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
                    fieldName + " must be between " + minVal + " and " + maxVal);
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
