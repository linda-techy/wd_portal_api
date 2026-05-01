package com.wd.api.estimation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FloorDto(
        @NotBlank String floorName,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal length,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal width) {}
