package com.wd.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.FeedbackAnalyticsDto;
import com.wd.api.model.FeedbackForm;
import com.wd.api.model.FeedbackResponse;
import com.wd.api.repository.FeedbackFormRepository;
import com.wd.api.repository.FeedbackResponseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema-aware read-only analytics over collected feedback responses.
 *
 * <h3>Schema convention</h3>
 * <p>{@code FeedbackForm.formSchema} is a JSON array of question definitions:
 * <pre>
 *   [ {"key":"q1","label":"Overall satisfaction","type":"rating"}, ... ]
 * </pre>
 * Supported {@code type} values:
 * <ul>
 *   <li>{@code rating}  — integer 1–5 star score</li>
 *   <li>{@code nps}     — integer 0–10 promoter score</li>
 *   <li>{@code scale}   — any integer numeric scale</li>
 *   <li>{@code text}    — free-form string (excluded from numeric stats)</li>
 *   <li>{@code choice}  — string selection (excluded from numeric stats)</li>
 * </ul>
 *
 * <p>Each submitted {@code FeedbackResponse.responseData} is a JSON object keyed
 * by question key, e.g. {@code {"q1": 4, "q2": "Great"}}.
 *
 * <p>When {@code formSchema} is absent or empty this service falls back to
 * discovering numeric keys dynamically from the collected {@code responseData}
 * payloads, treating them as generic rating questions.
 *
 * <p>This service is strictly read-only — it never modifies any entity.
 */
@Service
@RequiredArgsConstructor
public class FeedbackAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackAnalyticsService.class);

    private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    private static final java.util.Set<String> NUMERIC_TYPES =
            java.util.Set.of("rating", "nps", "scale");

    private final FeedbackFormRepository formRepository;
    private final FeedbackResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Compute analytics for a single feedback form.
     *
     * @param formId the form to analyse
     * @return populated {@link FeedbackAnalyticsDto}
     * @throws IllegalArgumentException if the form is not found
     */
    @Transactional(readOnly = true)
    public FeedbackAnalyticsDto analyseForm(Long formId) {
        FeedbackForm form = formRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback form not found: " + formId));

        List<FeedbackResponse> responses =
                responseRepository.findByFormIdOrderBySubmittedAtDesc(formId);

        return buildAnalytics(formId, form.getFormSchema(), responses);
    }

    /**
     * Compute a project-level rollup across all forms for {@code projectId}.
     *
     * @param projectId the project to summarise
     * @return {@link FeedbackAnalyticsDto} with {@code formId == null}
     */
    @Transactional(readOnly = true)
    public FeedbackAnalyticsDto analyseProject(Long projectId) {
        List<FeedbackForm> forms =
                formRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        // Merge all responses from every form in the project
        List<FeedbackResponse> allResponses = new ArrayList<>();
        String mergedSchema = null;
        for (FeedbackForm form : forms) {
            allResponses.addAll(
                    responseRepository.findByFormIdOrderBySubmittedAtDesc(form.getId()));
            // Use the first non-null schema as the reference for key/label/type discovery
            if (mergedSchema == null && form.getFormSchema() != null) {
                mergedSchema = form.getFormSchema();
            }
        }

        return buildAnalytics(null, mergedSchema, allResponses);
    }

    // ── core analytics ────────────────────────────────────────────────────────

    private FeedbackAnalyticsDto buildAnalytics(Long formId,
                                                String formSchemaJson,
                                                List<FeedbackResponse> responses) {

        // Parse question definitions from formSchema
        List<QuestionDef> questions = parseSchema(formSchemaJson);

        // Parse each response's data, skipping malformed rows
        List<Map<String, Object>> parsedResponses = new ArrayList<>();
        for (FeedbackResponse response : responses) {
            Map<String, Object> data = parseResponseData(response.getResponseData());
            if (data != null) {
                parsedResponses.add(data);
            }
        }

        // If schema is empty, derive question defs dynamically from response data
        if (questions.isEmpty()) {
            questions = deriveQuestionsFromResponses(parsedResponses);
        }

        // Aggregate per numeric question
        List<FeedbackAnalyticsDto.QuestionStat> perQuestion = new ArrayList<>();
        Double npsScore = null;

        for (QuestionDef q : questions) {
            if ("text".equalsIgnoreCase(q.type()) || "choice".equalsIgnoreCase(q.type())) {
                continue; // skip non-numeric
            }

            // Collect numeric values for this question key
            List<Integer> values = new ArrayList<>();
            for (Map<String, Object> data : parsedResponses) {
                Object raw = data.get(q.key());
                if (raw instanceof Number num) {
                    values.add(num.intValue());
                }
            }

            // Build distribution map (value → count)
            Map<Integer, Long> distribution = new LinkedHashMap<>();
            for (Integer v : values) {
                distribution.merge(v, 1L, Long::sum);
            }

            Double avg = values.isEmpty() ? null
                    : values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            Double min = values.isEmpty() ? null
                    : (double) values.stream().mapToInt(Integer::intValue).min().orElse(0);
            Double max = values.isEmpty() ? null
                    : (double) values.stream().mapToInt(Integer::intValue).max().orElse(0);

            perQuestion.add(new FeedbackAnalyticsDto.QuestionStat(
                    q.key(), q.label(), q.type(), values.size(), avg, min, max, distribution));

            // NPS calculation: only for nps-type question
            if ("nps".equalsIgnoreCase(q.type()) && !values.isEmpty()) {
                long promoters  = values.stream().filter(v -> v >= 9).count();
                long detractors = values.stream().filter(v -> v <= 6).count();
                int total = values.size();
                npsScore = (double) (promoters - detractors) / total * 100.0;
            }
        }

        return new FeedbackAnalyticsDto(formId, responses.size(), perQuestion, npsScore);
    }

    // ── schema parsing ────────────────────────────────────────────────────────

    private List<QuestionDef> parseSchema(String formSchemaJson) {
        if (formSchemaJson == null || formSchemaJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(formSchemaJson, LIST_MAP_TYPE);
            List<QuestionDef> defs = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                String key   = asString(raw.get("key"));
                String label = asString(raw.get("label"));
                String type  = asString(raw.get("type"));
                if (key != null && type != null) {
                    defs.add(new QuestionDef(key, label != null ? label : key, type));
                }
            }
            return defs;
        } catch (Exception e) {
            log.warn("Could not parse formSchema (ignored): {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * When no schema is defined, discover numeric question keys by scanning
     * all response data maps and treating any key whose value is always
     * numeric as a generic "rating" question.
     */
    private List<QuestionDef> deriveQuestionsFromResponses(List<Map<String, Object>> responses) {
        // Collect keys where at least one response has a numeric value
        Map<String, Boolean> numericKeys = new LinkedHashMap<>();
        for (Map<String, Object> data : responses) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    numericKeys.putIfAbsent(entry.getKey(), true);
                }
            }
        }
        List<QuestionDef> defs = new ArrayList<>();
        for (String key : numericKeys.keySet()) {
            defs.add(new QuestionDef(key, key, "rating"));
        }
        return defs;
    }

    // ── response data parsing ─────────────────────────────────────────────────

    private Map<String, Object> parseResponseData(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            log.warn("Skipping malformed responseData: {}", e.getMessage());
            return null;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String asString(Object o) {
        return o instanceof String s ? s : null;
    }

    /** Immutable question definition parsed from formSchema. */
    private record QuestionDef(String key, String label, String type) {}
}
