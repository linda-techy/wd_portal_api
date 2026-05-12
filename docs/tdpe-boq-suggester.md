# Test-Driven Prompt Engineering — BOQ item suggester

This document explains how the AI-assisted "BOQ item suggester" feature
in `wd_portal_api` is built using Test-Driven Prompt Engineering (TDPE), how
to evolve the prompt safely, and how to wire it to a real Claude model.

The feature: a site engineer or estimator types "RCC slab for ground floor,
4-inch thickness" into the BOQ create dialog, and the assistant fills in
description, unit, HSN/SAC code, and item kind. The hard parts are tax-
invoice correctness (HSN/SAC must be valid 4-8 digit codes — see the G-21
backend fix) and BASE-vs-ADDON classification.

## TDPE in one paragraph

Treat prompts like code. Freeze each version. Run an automated eval harness
against a golden dataset that describes *constraints on correct output*
(regex, allow-lists, must-contain rules) rather than the exact byte-for-byte
response. When pass-rate plateaus, ship a new prompt version — don't edit
the old one. Tests live alongside the prompt, in the same module. The same
harness runs against a deterministic in-memory provider (CI / local dev) and
the real Claude API (release gates / production drift detection).

## Layout

```
src/main/java/com/wd/api/ai/
├── LlmProvider.java                    // interface (the TDPE seam)
├── LlmRequest.java / LlmResponse.java  // provider-agnostic shapes
├── StubLlmProvider.java                // default — feature ships dark
├── AnthropicLlmProvider.java           // real Claude — @ConditionalOnProperty(ai.anthropic.enabled=true)
├── prompt/
│   ├── PromptVersion.java              // V1_MINIMAL, V2_FEW_SHOT, V3_DOMAIN_GROUNDED
│   └── BoqSuggesterPrompt.java         // the three frozen prompts
├── BoqSuggesterRequest.java
├── BoqSuggesterResponse.java
├── BoqSuggesterService.java            // orchestrator (provider-agnostic)
└── BoqSuggesterController.java         // POST /api/ai/boq/suggest (PreAuthorize BOQ_CREATE)

src/test/java/com/wd/api/ai/
├── BoqSuggesterPromptEvalTest.java     // the TDPE story (v1 red → v3 green)
└── eval/
    ├── ScriptedLlmProvider.java        // deterministic in-memory provider
    ├── EvalCase.java                   // constraints-as-data
    ├── EvalReport.java / EvalResult.java
    └── PromptEvalRunner.java

src/test/resources/ai/
├── boq-suggester-golden-cases.json     // dataset — append-only
└── scripted-responses.json             // canned model outputs per (version, case)
```

## The TDPE story (read the test output)

`mvn -Dtest=BoqSuggesterPromptEvalTest test` prints:

```
=== TDPE eval — V1_MINIMAL ===
Pass rate: 1 / 6 (17%)
  [FAIL] rcc_slab_ground_floor
        - hsnSacCode '9954' does not match ^9954[0-9]{2}$
  [FAIL] modular_kitchen_island
        - hsnSacCode '9403' does not match ^9954[0-9]{2}$
        - itemKind 'BASE' not in [ADDON, OPTIONAL]
  ...

=== TDPE eval — V2_FEW_SHOT ===
Pass rate: 3 / 6 (50%)
  [PASS] rcc_slab_ground_floor
  [PASS] modular_kitchen_island
  [FAIL] external_plastering
        - hsnSacCode '995411' does not match ^995453$
  ...

=== TDPE eval — V3_DOMAIN_GROUNDED ===
Pass rate: 6 / 6 (100%)
  [PASS] rcc_slab_ground_floor
  [PASS] modular_kitchen_island
  ...
```

Four JUnit assertions enforce the story:

1. **V1 is red.** Pass rate < 50 %. The `modular_kitchen_island` case is
   explicitly listed as failing because the minimal prompt has no way to
   teach the model that customer upgrades are `ADDON`, not `BASE`.
2. **V2 improves over V1.** Few-shot examples teach the JSON fence and the
   `9954xx` range — but specific SAC codes (995453, 995461, 995455) are
   still wrong because V2 doesn't include the full catalogue.
3. **V3 is green.** Pass rate = 100 %. The full HSN/SAC catalogue and the
   item-kind decision rules close the gap.
4. **Monotonicity.** V1 < V2 ≤ V3. Nothing regresses.

## How to add a new eval case

1. Add an entry to `src/test/resources/ai/boq-suggester-golden-cases.json`:
   ```json
   {
     "name": "marble_flooring_premium",
     "input": "Italian marble flooring 18mm thick, polished",
     "expectations": {
       "descriptionMustContainAny": ["marble"],
       "unitOneOf": ["m2", "sft", "sqm"],
       "hsnSacRegex": "^99545[5-6]$",
       "itemKindOneOf": ["ADDON"]
     }
   }
   ```
2. Add scripted responses for **every existing prompt version** to
   `scripted-responses.json` — what would V1 / V2 / V3 plausibly answer? If
   you don't know, run the real Anthropic provider once (see below), copy
   the actual answer, and curate it.
3. Re-run the test. If the case fails V3, that's the signal — write a new
   `V4_…` prompt or improve the catalogue in V3.

## How to evolve the prompt

1. **Never edit V3.** Add `V4_…` to `PromptVersion` and `BoqSuggesterPrompt`.
2. Add a new `@Test v4_…_passes` to `BoqSuggesterPromptEvalTest`.
3. Run all tests — V4 must beat or match V3 on every case, plus pass the
   new case(s) that motivated V4.
4. Flip the default by changing `ai.boq-suggester.prompt-version: V4_…` in
   `application.yml`. Old prompts stay in code as historical record.

## How to run the real Claude provider (release-gate eval)

```yaml
# application-anthropic.yml
ai:
  anthropic:
    enabled: true
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
ai:
  model:
    opus: claude-opus-4-7
```

Then run the suggester end-to-end against a live model:

```bash
ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run -Dspring-boot.run.profiles=anthropic
```

For batch eval against the real model (release gate), write a separate
`@IntegrationTest` that wires `AnthropicLlmProvider` directly (without
ScriptedLlmProvider) and runs `PromptEvalRunner.run(V3)`. Compare actual
pass-rate to the scripted-stub baseline — divergence > a few percent means
either the model has drifted or your scripted answers are out of date.

## What we deliberately did *not* do

- **No `output_config.format` / structured outputs binding** — the prompt
  asks for a fenced JSON block and the service parses it. Keeps the
  provider binding surface small and portable. Lift to native structured
  outputs once the eval baseline is stable.
- **No prompt caching** — added next, when traffic justifies it. The V3
  system prompt is ~1.5 KB and stable across requests, a perfect cache
  candidate.
- **No retry / backoff in the provider** — the Anthropic Java SDK retries
  429s and 5xxs automatically with exponential backoff.

## Production safety notes

- Default deployment is the **stub** provider: feature ships dark, no
  tokens spent.
- Endpoint is gated by `BOQ_CREATE` — same right as actually adding the
  item. AI suggestions don't escalate privilege.
- HSN/SAC and item-kind values are validated server-side in
  `BoqSuggesterService.validate*()` — a misbehaving model can't smuggle a
  malformed code through.
- The Flutter UI shows the suggestion in a review card before applying;
  nothing auto-fills without an explicit tap.
- All suggestions log `promptVersion`, `providerName`, and token usage for
  audit and cost analysis.
