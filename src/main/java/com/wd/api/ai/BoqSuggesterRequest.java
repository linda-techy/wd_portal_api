package com.wd.api.ai;

import com.wd.api.ai.prompt.PromptVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoqSuggesterRequest(
        @NotBlank(message = "rawText is required")
        @Size(max = 500, message = "rawText must be 500 characters or less")
        String rawText,

        /** Optional override; null = current production version. */
        PromptVersion promptVersion
) {}
