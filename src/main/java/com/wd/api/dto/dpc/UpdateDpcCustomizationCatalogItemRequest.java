package com.wd.api.dto.dpc;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * PATCH-payload for an existing DPC-customization-catalog row.
 *
 * All fields are nullable — only non-null fields are applied to the entity.
 */
public record UpdateDpcCustomizationCatalogItemRequest(
        @Size(max = 80) String code,
        @Size(max = 255) String name,
        String description,
        @Size(max = 80) String category,
        @Size(max = 40) String unit,
        @DecimalMin("0") BigDecimal defaultAmount,
        Boolean isActive
) {
}
