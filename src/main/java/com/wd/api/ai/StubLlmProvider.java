package com.wd.api.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default LLM provider. Returns a deterministic placeholder so the AI feature
 * can ship dark behind a flag without leaking partial functionality or paying
 * for tokens by accident.
 *
 * <p>Activated only when {@code ai.anthropic.enabled} is absent or false AND
 * no other {@link LlmProvider} bean is in the context — production wires
 * {@link AnthropicLlmProvider} by setting {@code ai.anthropic.enabled=true};
 * tests wire {@code ScriptedLlmProvider} directly via {@code @MockBean} or
 * a test-config class, which trips the {@code @ConditionalOnMissingBean}.
 */
@Component
@ConditionalOnProperty(name = "ai.anthropic.enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnMissingBean(value = LlmProvider.class, ignored = StubLlmProvider.class)
public class StubLlmProvider implements LlmProvider {

    @Override
    public LlmResponse complete(LlmRequest request) {
        // Return a syntactically valid response so callers' JSON parsing
        // succeeds, but with a UI-visible marker so nobody mistakes it for
        // real suggestions in a screenshot.
        String body = """
                ```json
                {
                  "description": "AI suggestions disabled — set ai.anthropic.enabled=true.",
                  "unit": "nos",
                  "hsnSacCode": "9954",
                  "itemKind": "BASE",
                  "confidence": "stub",
                  "reasoning": "StubLlmProvider returned a placeholder. No model was called."
                }
                ```
                """;
        return new LlmResponse(body, "stub", 0L, 0L, 0L, 0L, name());
    }

    @Override
    public String name() {
        return "stub";
    }
}
