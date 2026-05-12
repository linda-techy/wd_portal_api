package com.wd.api.ai.eval;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One golden case. Loaded from boq-suggester-golden-cases.json.
 *
 * <p>Expectations are <i>constraints</i> rather than exact-match values —
 * the model picks the wording, we judge whether it satisfies the rule. This
 * is what makes the dataset useful across many prompt iterations: V1 and V3
 * will rarely produce byte-identical descriptions, but both can be checked
 * against the same "must contain RCC or slab" rule.
 */
public record EvalCase(
        String name,
        String input,
        List<String> descriptionMustContainAny,
        List<String> unitOneOf,
        Pattern hsnSacRegex,
        List<String> itemKindOneOf
) {

    public static EvalCase fromJson(JsonNode node) {
        JsonNode exp = node.get("expectations");
        return new EvalCase(
                node.get("name").asText(),
                node.get("input").asText(),
                stringList(exp, "descriptionMustContainAny"),
                stringList(exp, "unitOneOf"),
                Pattern.compile(exp.get("hsnSacRegex").asText()),
                stringList(exp, "itemKindOneOf")
        );
    }

    private static List<String> stringList(JsonNode parent, String field) {
        List<String> out = new ArrayList<>();
        JsonNode arr = parent.get(field);
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
