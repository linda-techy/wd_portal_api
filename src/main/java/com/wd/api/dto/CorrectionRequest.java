package com.wd.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for correcting (reducing) a BOQ item's executed or billed quantity.
 * Admin-only operation — requires reason and optional document reference for audit trail.
 */
public record CorrectionRequest(

        @NotNull(message = "Correction type is required")
        CorrectionType type,

        @NotNull(message = "Correction amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Reason is required for correction")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason,

        String referenceNumber // Optional document reference for audit trail

) {
    public enum CorrectionType {
        REDUCE_EXECUTION,
        REDUCE_BILLING
    }
}
