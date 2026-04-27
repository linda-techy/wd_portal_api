package com.wd.api.dto.quotation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to promote an ad-hoc quotation line item into the master catalog.
 *
 * <p>{@code code} is optional — when blank, the service derives it from
 * {@code name} (uppercased, non-alphanumeric -> '-', truncated to 80).
 * {@code defaultUnitPrice} is optional — when null, falls back to the
 * source line item's {@code unitPrice}.
 */
public record PromoteItemToCatalogRequest(
        @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 80) String category,
        @Size(max = 40) String unit,
        BigDecimal defaultUnitPrice
) {
}
