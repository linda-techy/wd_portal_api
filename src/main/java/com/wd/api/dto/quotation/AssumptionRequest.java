package com.wd.api.dto.quotation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record AssumptionRequest(
        @PositiveOrZero Integer displayOrder,
        @NotBlank String text
) {
}
