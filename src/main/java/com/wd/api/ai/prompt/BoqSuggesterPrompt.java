package com.wd.api.ai.prompt;

import com.wd.api.ai.LlmRequest;

/**
 * Builds {@link LlmRequest}s for the "free-text → BOQ-item suggestion" task.
 *
 * <p>The three versions here are the actual TDPE artefacts: each version is
 * frozen, the eval harness records pass-rates against the same golden
 * dataset, and improvements ship as a new {@link PromptVersion} rather than
 * an in-place edit. The narrative the evals show:
 *
 * <ul>
 *   <li><b>V1</b> — minimal one-liner. Often returns OK-ish descriptions but
 *       hallucinates HSN/SAC codes or omits them entirely.</li>
 *   <li><b>V2</b> — adds two worked examples (one structural, one interior)
 *       so the model picks up the JSON-fence convention and starts using
 *       service codes (995411, 995425) rather than guessing.</li>
 *   <li><b>V3</b> — full system prompt with the India HSN/SAC service-code
 *       catalogue and {@code itemKind} decision rules. Highest pass-rate.</li>
 * </ul>
 *
 * <p>Output contract (all versions): the model returns JSON inside a fenced
 * code block with keys
 * {@code {description, unit, hsnSacCode, itemKind, confidence, reasoning}}.
 * Parsing happens in {@code BoqSuggesterService} above this layer.
 */
public final class BoqSuggesterPrompt {

    private static final int MAX_TOKENS = 800;

    private BoqSuggesterPrompt() {}

    public static LlmRequest build(PromptVersion version, String userText) {
        return switch (version) {
            case V1_MINIMAL          -> v1(userText);
            case V2_FEW_SHOT         -> v2(userText);
            case V3_DOMAIN_GROUNDED  -> v3(userText);
        };
    }

    // ── V1: minimal ─────────────────────────────────────────────────────────
    private static LlmRequest v1(String userText) {
        String system = """
                You convert free-text descriptions of Indian construction work
                into a BOQ item. Return JSON inside a ```json fenced block with
                keys: description, unit, hsnSacCode, itemKind, confidence, reasoning.
                """;
        return new LlmRequest(system, userText, "opus", MAX_TOKENS, "V1_MINIMAL");
    }

    // ── V2: few-shot ────────────────────────────────────────────────────────
    private static LlmRequest v2(String userText) {
        String system = """
                You convert free-text descriptions of Indian construction work
                into a BOQ item.

                Return JSON inside a ```json fenced block with keys:
                description (string, ≤120 chars), unit (string), hsnSacCode
                (4-8 digits as string), itemKind (BASE | ADDON | OPTIONAL |
                EXCLUSION), confidence (high | medium | low), reasoning (≤200
                chars).

                Example 1
                Input: RCC slab work for ground floor, 4-inch thickness
                Output:
                ```json
                {
                  "description": "RCC slab for ground floor, 100mm thick",
                  "unit": "m3",
                  "hsnSacCode": "995411",
                  "itemKind": "BASE",
                  "confidence": "high",
                  "reasoning": "Structural concrete service, SAC 995411."
                }
                ```

                Example 2
                Input: modular kitchen with island and quartz top
                Output:
                ```json
                {
                  "description": "Modular kitchen with island and quartz countertop",
                  "unit": "lot",
                  "hsnSacCode": "995473",
                  "itemKind": "ADDON",
                  "confidence": "medium",
                  "reasoning": "Interior fit-out service, SAC 995473."
                }
                ```
                """;
        return new LlmRequest(system, userText, "opus", MAX_TOKENS, "V2_FEW_SHOT");
    }

    // ── V3: domain-grounded ─────────────────────────────────────────────────
    private static LlmRequest v3(String userText) {
        String system = """
                You are a BOQ assistant for an Indian residential / commercial
                construction firm. Convert one line of free-text into a single,
                tax-invoice-ready BOQ item.

                ## Output

                Return JSON only, inside a ```json fenced block, with keys:
                  description    string, ≤120 chars, sentence case, no marketing fluff
                  unit           one of: m3, m2, sft, rft, kg, MT, nos, lot, point
                  hsnSacCode     4-8 digits as a string; prefer 6-digit SAC for services
                  itemKind       BASE | ADDON | OPTIONAL | EXCLUSION
                  confidence     high | medium | low
                  reasoning      ≤200 chars; cite which catalogue entry you used

                ## India HSN / SAC catalogue (use these first; only invent when none fits)

                Construction services (SAC, 6 digits):
                  995411  General construction services of single-/multi-dwelling residential buildings
                  995412  General construction services of other residential buildings (hostels, etc.)
                  995421  General construction services of highways, streets, roads
                  995425  Installation services of industrial, manufacturing & service machinery
                  995432  Site preparation services
                  995441  Excavation and earthmoving services
                  995451  Concrete work (RCC, PCC)
                  995452  Glazing services
                  995453  Plastering
                  995454  Painting services
                  995455  Floor / wall tiling, marble, granite
                  995456  Other building completion / finishing
                  995461  Electrical installation services
                  995462  Water plumbing and drain laying
                  995463  Heating, ventilation and air-conditioning installation
                  995464  Gas fitting installation
                  995465  Other installation services
                  995471  Other building cleaning services
                  995473  Specialised completion / finishing — modular kitchens, false ceilings, joinery
                  995478  Other building completion and finishing services
                  995479  Other site preparation / construction services n.e.c.

                Goods (HSN, 4-8 digits) used in construction:
                  6810    Articles of cement, concrete
                  6907    Ceramic tiles
                  6810    Precast concrete units
                  7214    TMT steel bars
                  2523    Portland cement
                  4407    Sawn timber

                ## itemKind decision rules

                  BASE       — always included in the contract scope (structural work,
                                MEP rough-in, basic finishes)
                  ADDON      — premium/customer-selected upgrade above base (modular
                                kitchen, designer lights, premium tiles)
                  OPTIONAL   — explicitly optional; not in the base sum
                  EXCLUSION  — listed for transparency, not delivered

                ## Tone

                Concise. No emojis. No marketing phrasing. Don't pad descriptions
                with adjectives like "premium" or "high-quality" unless the user
                wrote them.

                ## Worked example

                Input: RCC slab work for ground floor, 4-inch thickness
                Output:
                ```json
                {
                  "description": "RCC slab for ground floor, 100 mm thick",
                  "unit": "m3",
                  "hsnSacCode": "995451",
                  "itemKind": "BASE",
                  "confidence": "high",
                  "reasoning": "Concrete work — SAC 995451 (RCC/PCC). m3 because slab volume is the contractual unit."
                }
                ```
                """;
        return new LlmRequest(system, userText, "opus", MAX_TOKENS, "V3_DOMAIN_GROUNDED");
    }
}
