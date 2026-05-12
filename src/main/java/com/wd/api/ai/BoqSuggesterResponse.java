package com.wd.api.ai;

/**
 * Shape returned to the caller. Mirrors the JSON the model emits, plus
 * {@code promptVersion} so the client can A/B compare versions and
 * {@code providerName} so traces show whether a stub or real model ran.
 */
public record BoqSuggesterResponse(
        String description,
        String unit,
        String hsnSacCode,
        String itemKind,
        String confidence,
        String reasoning,
        String promptVersion,
        String providerName,
        long inputTokens,
        long outputTokens,
        long latencyMs
) {}
