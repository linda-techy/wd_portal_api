package com.wd.api.estimation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EstimationSubResourceRequest(

        @NotBlank(message = "label is required")
        @Size(max = 200, message = "label must not exceed 200 characters")
        String label,

        String description,

        Integer displayOrder,

        BigDecimal percentage
) {
    public EstimationSubResourceRequest {
        if (displayOrder == null) displayOrder = 0;
    }
}
