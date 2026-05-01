package com.wd.api.estimation.service.calc.view;

import java.math.BigDecimal;

public record CalculationConstants(
        BigDecimal semiCoveredFactor,
        BigDecimal openTerraceFactor,
        BigDecimal customisationDriftThreshold) {}
