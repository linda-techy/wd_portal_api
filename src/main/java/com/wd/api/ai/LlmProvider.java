package com.wd.api.ai;

/**
 * Single-call chat-completion abstraction.
 *
 * <p>This is the seam the eval harness pivots on: tests inject a deterministic
 * {@code ScriptedLlmProvider}; production wires either {@code StubLlmProvider}
 * (default, returns a placeholder so the feature flag can ship dark) or
 * {@code AnthropicLlmProvider} (real Claude calls behind the
 * {@code ai.anthropic.enabled=true} property).
 *
 * <p>Keep this interface stable. Eval cases assume identical behaviour across
 * providers; widening the surface tends to make the stub diverge from the real
 * provider and silently invalidate eval results.
 */
public interface LlmProvider {

    LlmResponse complete(LlmRequest request);

    /** Identifier used in logs and eval reports. */
    String name();
}
