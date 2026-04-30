package com.wd.api.dto.quotation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create / update payload for a {@code quotation_payment_milestones} row.
 *
 * <p>{@code amount} is intentionally optional — the BUDGETARY stage publishes
 * the structural payment shape (e.g. "10% on agreement, 15% at plinth, …")
 * without rupee figures. Service-layer validation enforces the
 * "sum-of-percentages = 100" rule across siblings on save.
 */
public record PaymentMilestoneRequest(
        @NotNull @PositiveOrZero Integer milestoneNumber,
        @NotBlank @Size(max = 120) String triggerEvent,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal percentage,
        @DecimalMin("0") BigDecimal amount,
        String notes
) {
}
