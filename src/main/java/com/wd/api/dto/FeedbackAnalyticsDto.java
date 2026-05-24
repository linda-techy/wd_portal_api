package com.wd.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for schema-aware feedback analytics.
 *
 * <p>JSON shape:
 * <pre>
 * {
 *   "formId": 1,                    // null for project-level rollup
 *   "totalResponses": 42,
 *   "perQuestion": [
 *     {
 *       "key": "overall",
 *       "label": "Overall satisfaction",
 *       "type": "rating",
 *       "count": 42,
 *       "average": 4.2,
 *       "min": 1.0,
 *       "max": 5.0,
 *       "distribution": { 1: 0, 2: 3, 3: 8, 4: 18, 5: 13 }
 *     }
 *   ],
 *   "nps": 42.3                      // present only when an nps-type question exists
 * }
 * </pre>
 *
 * <p>Question types that produce numeric stats: {@code rating} (1–5),
 * {@code nps} (0–10), {@code scale} (any integer).
 * Types {@code text} and {@code choice} are excluded from {@code perQuestion}.
 */
public record FeedbackAnalyticsDto(
        Long formId,
        int totalResponses,
        List<QuestionStat> perQuestion,
        Double nps
) {

    /**
     * Per-question aggregate statistics.
     *
     * <p>{@code average}, {@code min}, {@code max} are {@code null} when
     * {@code count == 0} (no responses contributed a value for this question).
     */
    public record QuestionStat(
            String key,
            String label,
            String type,
            int count,
            Double average,
            Double min,
            Double max,
            Map<Integer, Long> distribution
    ) {}
}
