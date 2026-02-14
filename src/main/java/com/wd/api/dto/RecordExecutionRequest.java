package com.wd.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RecordExecutionRequest(
        @NotNull(message = "Execution quantity is required")
        @DecimalMin(value = "0.0001", message = "Execution quantity must be greater than 0")
        BigDecimal quantity,

        @Size(max = 255, message = "Reference must not exceed 255 characters")
        String reference,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {
}
