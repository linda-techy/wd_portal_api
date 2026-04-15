package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateVariationRequest(
    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Estimated amount is required")
    @Positive(message = "Estimated amount must be positive")
    BigDecimal estimatedAmount,

    Long createdById
) {}
