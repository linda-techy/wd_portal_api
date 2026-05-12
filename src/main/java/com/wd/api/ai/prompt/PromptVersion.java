package com.wd.api.ai.prompt;

/**
 * Versioned prompt identities. Bumping a version is how TDPE distinguishes
 * an old prompt's eval result from a new one — every {@link com.wd.api.ai.LlmRequest}
 * carries the version so eval logs, traces, and the scripted-stub harness
 * can correlate output quality with the prompt that produced it.
 *
 * <p>Convention: <b>append a version, never edit an existing one</b>. If you
 * change v2's text after eval results have been recorded against it, the
 * dataset becomes meaningless. Add v4 and re-run.
 */
public enum PromptVersion {
    /** Minimal prompt: just the task, no examples, no domain context. */
    V1_MINIMAL,
    /** Adds 2 few-shot examples (RCC + interior). */
    V2_FEW_SHOT,
    /** Adds India construction-domain system context (HSN/SAC catalog, ItemKind rules). */
    V3_DOMAIN_GROUNDED
}
