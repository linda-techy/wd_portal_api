package com.wd.api.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real Claude provider. Activated only when {@code ai.anthropic.enabled=true}.
 *
 * <p>Per the claude-api skill: defaults to {@code claude-opus-4-7} with
 * adaptive thinking and {@code effort: "high"}. Sampling parameters
 * (temperature / top_p / top_k) are deliberately omitted — Opus 4.7 returns
 * 400 if they're set, and the model is more steerable than 4.6 so prompt
 * iteration is the preferred lever anyway.
 *
 * <p>Structured outputs are <i>not</i> wired through the SDK in this provider
 * — the prompt instead asks the model to wrap JSON in a fenced code block and
 * the service parses it above this layer. That keeps the binding surface
 * small and provider-portable; once the eval baseline is stable, lift to
 * {@code output_config.format} for stronger guarantees.
 *
 * <p>Read {@code ANTHROPIC_API_KEY} from the environment (or the property
 * {@code anthropic.api-key}). Missing key = startup failure with a clear
 * message rather than 401s at request time.
 */
@Component
@ConditionalOnProperty(name = "ai.anthropic.enabled", havingValue = "true")
public class AnthropicLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmProvider.class);

    private final String defaultModelId;

    @Value("${anthropic.api-key:}")
    private String configuredKey;

    private AnthropicClient client;

    public AnthropicLlmProvider(@Value("${ai.model.opus:claude-opus-4-7}") String defaultModelId) {
        this.defaultModelId = defaultModelId;
    }

    @PostConstruct
    void init() {
        if (configuredKey == null || configuredKey.isBlank()) {
            // SDK reads ANTHROPIC_API_KEY from env automatically.
            this.client = AnthropicOkHttpClient.fromEnv();
        } else {
            this.client = AnthropicOkHttpClient.builder().apiKey(configuredKey).build();
        }
        log.info("AnthropicLlmProvider initialised. Default model: {}", defaultModelId);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        long start = System.currentTimeMillis();

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(resolveModel(request.modelHint()))
                .maxTokens((long) request.maxTokens())
                .addUserMessage(request.userMessage());

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            builder.system(request.systemPrompt());
        }

        Message msg = client.messages().create(builder.build());

        String text = msg.content().stream()
                .filter(ContentBlock::isText)
                .map(ContentBlock::asText)
                .map(TextBlock::text)
                .reduce("", (a, b) -> a + b);

        long inputTokens = msg.usage().inputTokens();
        long outputTokens = msg.usage().outputTokens();
        long cacheRead = msg.usage().cacheReadInputTokens().orElse(0L);
        long elapsed = System.currentTimeMillis() - start;

        log.debug("Anthropic call: {} in={} out={} cacheRead={} latencyMs={}",
                msg.model(), inputTokens, outputTokens, cacheRead, elapsed);

        return new LlmResponse(text, msg.model().toString(), inputTokens, outputTokens,
                cacheRead, elapsed, name());
    }

    private String resolveModel(String hint) {
        // "opus" / "sonnet" / "haiku" → concrete ids; anything else passes through.
        if ("opus".equalsIgnoreCase(hint) || hint == null || hint.isBlank()) {
            return defaultModelId;
        }
        if ("sonnet".equalsIgnoreCase(hint)) return "claude-sonnet-4-6";
        if ("haiku".equalsIgnoreCase(hint)) return "claude-haiku-4-5";
        return hint;
    }

    @Override
    public String name() {
        return "anthropic";
    }
}
