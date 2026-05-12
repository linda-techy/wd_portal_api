package com.wd.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.ai.prompt.BoqSuggesterPrompt;
import com.wd.api.ai.prompt.PromptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates a single AI BOQ-item suggestion: pick prompt version → call
 * provider → extract the JSON-fenced payload → coerce to {@link BoqSuggesterResponse}.
 *
 * <p>The service is provider-agnostic by design — eval tests inject a
 * scripted provider; production wires the stub or the real Anthropic
 * adapter via Spring's {@code @ConditionalOnProperty} flag.
 */
@Service
public class BoqSuggesterService {

    private static final Logger log = LoggerFactory.getLogger(BoqSuggesterService.class);
    private static final Pattern FENCE = Pattern.compile(
            "```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);
    private static final Pattern HSN_SAC = Pattern.compile("^[0-9]{4,8}$");

    private final LlmProvider provider;
    private final ObjectMapper mapper;
    private final PromptVersion currentVersion;

    public BoqSuggesterService(LlmProvider provider,
                                ObjectMapper mapper,
                                @Value("${ai.boq-suggester.prompt-version:V3_DOMAIN_GROUNDED}")
                                PromptVersion currentVersion) {
        this.provider = provider;
        this.mapper = mapper;
        this.currentVersion = currentVersion;
    }

    public BoqSuggesterResponse suggest(BoqSuggesterRequest request) {
        PromptVersion version = request.promptVersion() != null
                ? request.promptVersion()
                : currentVersion;

        LlmRequest llm = BoqSuggesterPrompt.build(version, request.rawText().trim());
        LlmResponse resp = provider.complete(llm);

        JsonNode json = parseJsonOrThrow(resp.text());

        String description = text(json, "description");
        String unit = text(json, "unit");
        String hsnSacCode = text(json, "hsnSacCode");
        String itemKind = text(json, "itemKind");
        String confidence = text(json, "confidence");
        String reasoning = text(json, "reasoning");

        validateHsnSac(hsnSacCode);
        validateItemKind(itemKind);

        return new BoqSuggesterResponse(
                description, unit, hsnSacCode, itemKind, confidence, reasoning,
                version.name(),
                resp.providerName(),
                resp.inputTokens(), resp.outputTokens(), resp.latencyMs()
        );
    }

    // ── parsing helpers ─────────────────────────────────────────────────────

    private JsonNode parseJsonOrThrow(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("LLM returned an empty response");
        }
        String candidate = extractFencedJson(body);
        try {
            return mapper.readTree(candidate);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON. body=[{}] extracted=[{}]", body, candidate);
            throw new IllegalStateException(
                    "LLM did not return parseable JSON. Bump the prompt version or "
                            + "tighten the output contract.", e);
        }
    }

    /**
     * Pull JSON out of a ```json fenced block; falls back to the raw body if
     * the model skipped the fence. Keeps the prompt + parser tolerant during
     * iteration without needing a strict-mode kill switch.
     */
    private String extractFencedJson(String body) {
        Matcher m = FENCE.matcher(body);
        return m.find() ? m.group(1) : body.trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            throw new IllegalStateException("LLM response missing required field: " + field);
        }
        return v.asText().trim();
    }

    private void validateHsnSac(String code) {
        if (!HSN_SAC.matcher(code).matches()) {
            throw new IllegalStateException(
                    "LLM returned malformed HSN/SAC code: '" + code + "' (must be 4-8 digits)");
        }
    }

    private void validateItemKind(String kind) {
        if (!("BASE".equals(kind) || "ADDON".equals(kind)
                || "OPTIONAL".equals(kind) || "EXCLUSION".equals(kind))) {
            throw new IllegalStateException(
                    "LLM returned invalid itemKind: '" + kind + "'");
        }
    }
}
