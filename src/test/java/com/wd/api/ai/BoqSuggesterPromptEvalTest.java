package com.wd.api.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.ai.eval.EvalReport;
import com.wd.api.ai.eval.PromptEvalRunner;
import com.wd.api.ai.eval.ScriptedLlmProvider;
import com.wd.api.ai.prompt.PromptVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The TDPE story for the BOQ-item AI suggester.
 *
 * <p>Each test is a frozen quality contract for a prompt version. The
 * dataset lives in {@code src/test/resources/ai/boq-suggester-golden-cases.json};
 * the canned model outputs that simulate "what Claude would have said for
 * this version" live in {@code scripted-responses.json}. Real-model evals
 * follow the same shape: swap {@link ScriptedLlmProvider} for
 * {@code AnthropicLlmProvider} via the {@code ai.anthropic.enabled} flag
 * and re-run.
 *
 * <h2>How the story reads</h2>
 *
 * <pre>
 *   v1 minimal             → pass rate ~ 33 %  (HSN codes wrong / generic)
 *   v2 few-shot examples   → pass rate ~ 67 %  (descriptions land, HSN OK on most)
 *   v3 domain-grounded     → pass rate    100 %
 * </pre>
 *
 * <p>The assertions are inequalities, not "exact 67%": the dataset can grow
 * without rewriting these tests, as long as the <i>direction</i> of the
 * improvement is preserved.
 */
class BoqSuggesterPromptEvalTest {

    private ObjectMapper mapper;
    private PromptEvalRunner runner;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/ai/scripted-responses.json")) {
            Objects.requireNonNull(in, "scripted-responses.json not on classpath");
            JsonNode scripts = mapper.readTree(in);
            ScriptedLlmProvider provider = new ScriptedLlmProvider(scripts, mapper);
            // currentVersion arg doesn't matter — each test passes the version explicitly.
            BoqSuggesterService service = new BoqSuggesterService(
                    provider, mapper, PromptVersion.V3_DOMAIN_GROUNDED);
            runner = PromptEvalRunner.load(service, mapper);
        }
    }

    @Test
    @DisplayName("V1 (minimal prompt) — most cases fail: HSN codes generic, item-kind misclassified")
    void v1_minimal_is_red() {
        EvalReport report = runner.run(PromptVersion.V1_MINIMAL);
        System.out.println(report.summary());

        // V1 should be visibly bad — failures should outnumber passes.
        assertThat(report.passRate())
                .as("V1 pass rate is expected to be well below the V3 target")
                .isLessThan(0.5);

        // Worst offender: the modular-kitchen case should fail HSN/itemKind on V1
        assertThat(report.failingCaseNames())
                .contains("modular_kitchen_island");
    }

    @Test
    @DisplayName("V2 (few-shot) — improves over V1: examples teach the JSON contract + 9954xx range")
    void v2_few_shot_is_better_than_v1() {
        EvalReport v1 = runner.run(PromptVersion.V1_MINIMAL);
        EvalReport v2 = runner.run(PromptVersion.V2_FEW_SHOT);

        System.out.println(v2.summary());

        assertThat(v2.passRate())
                .as("V2 should beat V1 — that's the entire point of adding examples")
                .isGreaterThan(v1.passRate());

        // But V2 still doesn't have the SAC catalogue, so specific-code regexes
        // (e.g. '995453' for plastering) still miss. Pass rate < 1.0 here is
        // the proof that few-shot alone isn't enough.
        assertThat(v2.passRate())
                .as("V2 alone should not yet reach 100% — that's V3's job")
                .isLessThan(1.0);
    }

    @Test
    @DisplayName("V3 (domain-grounded) — passes every golden case (TDPE 'green')")
    void v3_domain_grounded_is_green() {
        EvalReport v3 = runner.run(PromptVersion.V3_DOMAIN_GROUNDED);

        System.out.println(v3.summary());

        assertThat(v3.passRate())
                .as("V3 with the full HSN/SAC catalogue should hit every case")
                .isEqualTo(1.0);
        assertThat(v3.failingCaseNames()).isEmpty();
    }

    @Test
    @DisplayName("Monotonic improvement: V1 < V2 ≤ V3 (no regression as prompts evolve)")
    void prompt_evolution_is_monotonic() {
        EvalReport v1 = runner.run(PromptVersion.V1_MINIMAL);
        EvalReport v2 = runner.run(PromptVersion.V2_FEW_SHOT);
        EvalReport v3 = runner.run(PromptVersion.V3_DOMAIN_GROUNDED);

        System.out.printf(
                "%nMonotonicity check%n  V1 %s (%.0f%%)%n  V2 %s (%.0f%%)%n  V3 %s (%.0f%%)%n",
                v1.shortLine(), v1.passRate() * 100,
                v2.shortLine(), v2.passRate() * 100,
                v3.shortLine(), v3.passRate() * 100);

        assertThat(v1.passRate()).isLessThan(v2.passRate());
        assertThat(v2.passRate()).isLessThanOrEqualTo(v3.passRate());
    }
}
