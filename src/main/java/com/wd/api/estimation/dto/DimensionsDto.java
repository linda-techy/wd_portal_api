package com.wd.api.estimation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record DimensionsDto(
        @Valid List<FloorDto> floors,
        @NotNull @DecimalMin("0.0") BigDecimal semiCoveredArea,
        @NotNull @DecimalMin("0.0") BigDecimal openTerraceArea) {}
