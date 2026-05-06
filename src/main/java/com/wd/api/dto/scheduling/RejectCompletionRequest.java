package com.wd.api.dto.scheduling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCompletionRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(min = 5, max = 500, message = "Rejection reason must be 5-500 characters")
        String reason) { }
