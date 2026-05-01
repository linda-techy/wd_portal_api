package com.wd.api.estimation.service.calc.view;

import java.math.BigDecimal;
import java.util.List;

public record Dimensions(
        List<Floor> floors,
        BigDecimal semiCoveredArea,
        BigDecimal openTerraceArea) {}
