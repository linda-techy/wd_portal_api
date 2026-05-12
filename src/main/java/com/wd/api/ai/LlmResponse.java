package com.wd.api.ai;

/**
 * Provider-agnostic response.
 *
 * @param text             Raw assistant text. JSON parsing (if any) happens above this layer.
 * @param modelId          The concrete model id the provider used (e.g. "claude-opus-4-7").
 * @param inputTokens      Tokens billed for the prompt (best effort; 0 for stubs).
 * @param outputTokens     Tokens billed for the completion (best effort; 0 for stubs).
 * @param cacheReadTokens  Tokens served from cache (best effort; 0 for stubs / cold first call).
 * @param latencyMs        Wall-clock latency of the provider call.
 * @param providerName     "anthropic", "stub", "scripted", etc. Useful for traces.
 */
public record LlmResponse(
        String text,
        String modelId,
        long inputTokens,
        long outputTokens,
        long cacheReadTokens,
        long latencyMs,
        String providerName
) {}
