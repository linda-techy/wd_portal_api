package com.wd.api.ai.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.ai.LlmProvider;
import com.wd.api.ai.LlmRequest;
import com.wd.api.ai.LlmResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Deterministic in-memory LLM provider for tests.
 *
 * <p>Returns a canned response keyed by {@code (promptVersion, caseName)}.
 * The case name is extracted from the user message via a {@code [case=NAME]}
 * suffix the eval runner adds — this is a test-only convention so the stub
 * stays decoupled from the prompt body.
 *
 * <p>Loading the script as JSON (rather than hard-coding) means new eval
 * cases / new prompt versions add data files, not code.
 */
public class ScriptedLlmProvider implements LlmProvider {

    private final Map<String, Map<String, JsonNode>> scripts;
    private final ObjectMapper mapper;

    public ScriptedLlmProvider(JsonNode scriptsRoot, ObjectMapper mapper) {
        this.mapper = mapper;
        this.scripts = parse(scriptsRoot.get("scripts"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, JsonNode>> parse(JsonNode scriptsNode) {
        if (scriptsNode == null) {
            throw new IllegalStateException("scripted-responses.json missing 'scripts' root");
        }
        return mapperConvert(scriptsNode);
    }

    private static Map<String, Map<String, JsonNode>> mapperConvert(JsonNode scriptsNode) {
        java.util.HashMap<String, Map<String, JsonNode>> out = new java.util.HashMap<>();
        var versions = scriptsNode.fields();
        while (versions.hasNext()) {
            var ventry = versions.next();
            java.util.HashMap<String, JsonNode> inner = new java.util.HashMap<>();
            var cases = ventry.getValue().fields();
            while (cases.hasNext()) {
                var c = cases.next();
                inner.put(c.getKey(), c.getValue());
            }
            out.put(ventry.getKey(), inner);
        }
        return out;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String caseName = extractCaseTag(request.userMessage());
        Map<String, JsonNode> byCase = scripts.get(request.promptVersion());
        if (byCase == null) {
            throw new IllegalStateException(
                    "No scripted responses for promptVersion=" + request.promptVersion());
        }
        JsonNode payload = byCase.get(caseName);
        if (payload == null) {
            throw new IllegalStateException(
                    "No scripted response for case '" + caseName + "' under version "
                            + request.promptVersion()
                            + ". Add it to src/test/resources/ai/scripted-responses.json.");
        }
        String body;
        try {
            body = "```json\n" + mapper.writeValueAsString(payload) + "\n```";
        } catch (Exception e) {
            throw new RuntimeException("Failed to render scripted response", e);
        }
        return new LlmResponse(body, "scripted", 0, 0, 0, 0, name());
    }

    /** Eval cases call the provider with the marker "[case=NAME] <text>". */
    static String extractCaseTag(String userMessage) {
        Objects.requireNonNull(userMessage);
        if (!userMessage.startsWith("[case=")) {
            throw new IllegalStateException(
                    "ScriptedLlmProvider received a user message without a [case=…] tag. "
                            + "Use PromptEvalRunner so the marker gets prepended.");
        }
        int end = userMessage.indexOf(']');
        return userMessage.substring("[case=".length(), end);
    }

    @Override
    public String name() {
        return "scripted";
    }
}
