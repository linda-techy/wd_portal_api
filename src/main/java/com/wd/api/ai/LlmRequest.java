package com.wd.api.ai;

/**
 * Provider-agnostic request to a chat LLM.
 *
 * <p>Keeps the surface small on purpose: a system prompt (stable, cacheable),
 * a user message (varies per request), a model hint, and generation knobs.
 * Anything fancier — tools, vision, structured outputs — is intentionally out
 * of scope for the TDPE demo so the eval harness stays portable across
 * providers and so the test stub doesn't drift from the real wire shape.
 *
 * @param systemPrompt  Stable instructions and context. Cached when provider supports it.
 * @param userMessage   The per-request input.
 * @param modelHint     Logical model name ("opus", "sonnet"). Provider maps to its concrete id.
 * @param maxTokens     Hard upper bound on response length.
 * @param promptVersion Tag carried through for eval logging / scripted-stub matching.
 */
public record LlmRequest(
        String systemPrompt,
        String userMessage,
        String modelHint,
        int maxTokens,
        String promptVersion
) {
    public LlmRequest {
        if (systemPrompt == null) systemPrompt = "";
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage is required");
        }
        if (modelHint == null || modelHint.isBlank()) modelHint = "opus";
        if (maxTokens <= 0) maxTokens = 1024;
        if (promptVersion == null) promptVersion = "unversioned";
    }
}
