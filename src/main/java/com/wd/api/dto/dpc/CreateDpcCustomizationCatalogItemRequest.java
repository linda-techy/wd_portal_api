package com.wd.api.dto.dpc;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create-payload for a new DPC-customization-catalog row.
 */
public record CreateDpcCustomizationCatalogItemRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 80) String category,
        @Size(max = 40) String unit,
        @NotNull @DecimalMin("0") BigDecimal defaultAmount
) {
}
