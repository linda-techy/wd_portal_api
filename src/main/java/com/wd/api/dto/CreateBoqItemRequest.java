package com.wd.api.dto;

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
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotNull(message = "Unit rate is required")
        @DecimalMin(value = "0.01", message = "Unit rate must be greater than 0")
        BigDecimal unitRate,

        Long materialId,

        @Size(max = 5000, message = "Specifications must not exceed 5000 characters")
        String specifications,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
