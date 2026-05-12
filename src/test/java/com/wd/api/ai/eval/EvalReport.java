package com.wd.api.ai.eval;

import java.util.List;
import java.util.stream.Collectors;

/** Aggregate report for one prompt version's run over the dataset. */
public record EvalReport(
        String promptVersion,
        int total,
        int passed,
        List<EvalResult> results
) {
    public double passRate() {
        return total == 0 ? 0.0 : (double) passed / total;
    }

    public List<String> failingCaseNames() {
        return results.stream().filter(EvalResult::failed)
                .map(EvalResult::caseName).toList();
    }

    /** Human-readable summary suitable for {@code System.out} or assertion messages. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n=== TDPE eval — %s ===%n", promptVersion));
        sb.append(String.format("Pass rate: %d / %d (%.0f%%)%n",
                passed, total, passRate() * 100));
        for (EvalResult r : results) {
            sb.append(String.format("  %s %s%n",
                    r.passed() ? "[PASS]" : "[FAIL]", r.caseName()));
            if (r.failed()) {
                for (String f : r.failures()) sb.append("        - ").append(f).append('\n');
            }
        }
        return sb.toString();
    }

    public String shortLine() {
        return results.stream()
                .map(r -> r.passed() ? "✓" : "✗")
                .collect(Collectors.joining());
    }
}
