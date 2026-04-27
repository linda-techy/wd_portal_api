package com.wd.api.dto.quotation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * PATCH-payload for an existing quotation-catalog row.
 *
 * All fields are nullable — only non-null fields are applied to the entity.
 */
public record UpdateQuotationCatalogItemRequest(
        @Size(max = 80) String code,
        @Size(max = 255) String name,
        String description,
        @Size(max = 80) String category,
        @Size(max = 40) String unit,
        @DecimalMin("0") BigDecimal defaultUnitPrice,
        Boolean isActive
) {
}
