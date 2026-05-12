package com.wd.api.ai.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.ai.BoqSuggesterRequest;
import com.wd.api.ai.BoqSuggesterResponse;
import com.wd.api.ai.BoqSuggesterService;
import com.wd.api.ai.prompt.PromptVersion;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs a {@link PromptVersion} over the golden dataset and reports pass-rate.
 *
 * <p>The runner is the closest thing TDPE has to a unit-test loop: it owns
 * the case → result projection so individual tests stay declarative. To add a
 * new prompt version, write a new {@code @Test} that calls
 * {@code runner.run(PromptVersion.V4_…)} and assert on the report.
 */
public class PromptEvalRunner {

    private final BoqSuggesterService service;
    private final List<EvalCase> cases;

    public PromptEvalRunner(BoqSuggesterService service, List<EvalCase> cases) {
        this.service = service;
        this.cases = cases;
    }

    public static PromptEvalRunner load(BoqSuggesterService service, ObjectMapper mapper) {
        try (InputStream in = PromptEvalRunner.class.getResourceAsStream(
                "/ai/boq-suggester-golden-cases.json")) {
            Objects.requireNonNull(in, "boq-suggester-golden-cases.json not on classpath");
            JsonNode root = mapper.readTree(in);
            List<EvalCase> cs = new ArrayList<>();
            root.get("cases").forEach(c -> cs.add(EvalCase.fromJson(c)));
            return new PromptEvalRunner(service, cs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load golden cases", e);
        }
    }

    public EvalReport run(PromptVersion version) {
        List<EvalResult> results = new ArrayList<>();
        int passed = 0;
        for (EvalCase c : cases) {
            // Tag the case name into the user message so the ScriptedLlmProvider
            // can find the right canned answer. The real Anthropic provider just
            // sees a slightly longer prompt — it has no effect on real model
            // quality and the tag is stripped by tests when scoring.
            String tagged = "[case=" + c.name() + "] " + c.input();
            BoqSuggesterResponse resp;
            List<String> failures = new ArrayList<>();
            try {
                resp = service.suggest(new BoqSuggesterRequest(tagged, version));
            } catch (Exception e) {
                results.add(new EvalResult(c.name(), false,
                        List.of("threw: " + e.getMessage()), null));
                continue;
            }
            score(c, resp, failures);
            boolean pass = failures.isEmpty();
            if (pass) passed++;
            results.add(new EvalResult(c.name(), pass, failures, resp));
        }
        return new EvalReport(version.name(), cases.size(), passed, results);
    }

    private void score(EvalCase c, BoqSuggesterResponse resp, List<String> failures) {
        // description: must contain at least one of the required substrings
        // (case-insensitive)
        if (!c.descriptionMustContainAny().isEmpty()) {
            String desc = resp.description().toLowerCase();
            boolean any = c.descriptionMustContainAny().stream()
                    .anyMatch(s -> desc.contains(s.toLowerCase()));
            if (!any) {
                failures.add("description did not contain any of "
                        + c.descriptionMustContainAny() + " (got: '" + resp.description() + "')");
            }
        }

        // unit: case-insensitive exact match against allow-list
        if (!c.unitOneOf().isEmpty()) {
            String unit = resp.unit().toLowerCase();
            boolean ok = c.unitOneOf().stream()
                    .map(String::toLowerCase)
                    .anyMatch(unit::equals);
            if (!ok) {
                failures.add("unit '" + resp.unit() + "' not in " + c.unitOneOf());
            }
        }

        // hsnSacCode: regex match (regex is what catches V1's "9954" vs V3's "995451")
        if (c.hsnSacRegex() != null
                && !c.hsnSacRegex().matcher(resp.hsnSacCode()).matches()) {
            failures.add("hsnSacCode '" + resp.hsnSacCode() + "' does not match "
                    + c.hsnSacRegex().pattern());
        }

        // itemKind: exact match against allow-list
        if (!c.itemKindOneOf().isEmpty()
                && !c.itemKindOneOf().contains(resp.itemKind())) {
            failures.add("itemKind '" + resp.itemKind() + "' not in "
                    + c.itemKindOneOf());
        }
    }
}
