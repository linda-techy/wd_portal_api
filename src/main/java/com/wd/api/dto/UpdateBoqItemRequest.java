package com.wd.api.dto;

import com.wd.api.model.enums.ItemKind;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateBoqItemRequest(
        Long categoryId,

        Long workTypeId,

        @Size(max = 50, message = "Item code must not exceed 50 characters")
        String itemCode,

        // G-21: GST HSN / SAC code. Null = no change; if supplied it must
        // match the same format enforced on create.
        @Pattern(regexp = "^[0-9]{4,8}$",
                message = "HSN/SAC code must be 4-8 digits (e.g. '995411' for construction services)")
        String hsnSacCode,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", message = "Unit rate cannot be negative")
        BigDecimal unitRate,

        Long materialId,

        @Size(max = 5000, message = "Specifications must not exceed 5000 characters")
        String specifications,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        // null = no change
        ItemKind itemKind
) {}
