package com.wd.api.logging;

import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data in Portal API logs.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}
    private static final String MASK = "****";

    private static final Pattern[] JSON_PATTERNS = buildJsonPatterns();
    private static final Pattern[] PARAM_PATTERNS = buildParamPatterns();

    private static Pattern[] buildJsonPatterns() {
        return java.util.Arrays.stream(LoggingConstants.SENSITIVE_FIELDS)
                .map(f -> Pattern.compile(
                        "(?i)(\"" + Pattern.quote(f) + "\"\\s*:\\s*\")([^\"]*)(\")",
                        Pattern.CASE_INSENSITIVE))
                .toArray(Pattern[]::new);
    }

    private static Pattern[] buildParamPatterns() {
        return java.util.Arrays.stream(LoggingConstants.SENSITIVE_FIELDS)
                .map(f -> Pattern.compile(
                        "(?i)(" + Pattern.quote(f) + "=)([^&\\s,}]+)",
                        Pattern.CASE_INSENSITIVE))
                .toArray(Pattern[]::new);
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) return input;
        String result = input;
        for (Pattern p : JSON_PATTERNS)  result = p.matcher(result).replaceAll("$1" + MASK + "$3");
        for (Pattern p : PARAM_PATTERNS) result = p.matcher(result).replaceAll("$1" + MASK);
        return result;
    }

    public static String maskAuthHeader(String value) {
        if (value == null) return null;
        return value.toLowerCase().startsWith("bearer ") ? "Bearer ****" : MASK;
    }
}
