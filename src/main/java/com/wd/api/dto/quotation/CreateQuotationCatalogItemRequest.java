package com.wd.api.dto.quotation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create-payload for a new quotation-catalog row.
 */
public record CreateQuotationCatalogItemRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 80) String category,
        @Size(max = 40) String unit,
        @NotNull @DecimalMin("0") BigDecimal defaultUnitPrice
) {
}
