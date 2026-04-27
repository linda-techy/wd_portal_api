package com.wd.api.dto.dpc;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request to add a new MANUAL customization line to a DPC, sourced from
 * the catalog.
 *
 * <p>{@code amountOverride} defaults to the catalog row's
 * {@code defaultAmount} when omitted. There is no quantity — DPC
 * customization lines are LUMP-SUM amounts.
 */
public record AddCustomizationFromCatalogRequest(
        @NotNull Long catalogItemId,
        @DecimalMin("0") BigDecimal amountOverride
) {
}
