package com.wd.api.dto.quotation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request to add a new line item to a quotation, sourced from the catalog.
 *
 * <p>{@code quantity} defaults to 1 and {@code unitPriceOverride} defaults to
 * the catalog row's {@code defaultUnitPrice} when omitted.
 */
public record AddItemFromCatalogRequest(
        @NotNull Long catalogItemId,
        @DecimalMin("0") BigDecimal quantity,
        @DecimalMin("0") BigDecimal unitPriceOverride
) {
}
