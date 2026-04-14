package com.wd.api.dto;

import com.wd.api.model.enums.ItemKind;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateBoqItemRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        Long categoryId,

        Long workTypeId,

        @Size(max = 50, message = "Item code must not exceed 50 characters")
        String itemCode,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotBlank(message = "Unit is required")
        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
        BigDecimal quantity,

        @NotNull(message = "Unit rate is required")
        @DecimalMin(value = "0.0", message = "Unit rate cannot be negative")
        BigDecimal unitRate,

        Long materialId,

        @Size(max = 5000, message = "Specifications must not exceed 5000 characters")
        String specifications,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        // Defaults to BASE when null
        ItemKind itemKind
) {}
