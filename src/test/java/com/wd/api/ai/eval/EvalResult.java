package com.wd.api.ai.eval;

import com.wd.api.ai.BoqSuggesterResponse;

import java.util.List;

/** Per-case eval outcome. */
public record EvalResult(
        String caseName,
        boolean passed,
        List<String> failures,
        BoqSuggesterResponse response
) {
    public boolean failed() { return !passed; }
}
