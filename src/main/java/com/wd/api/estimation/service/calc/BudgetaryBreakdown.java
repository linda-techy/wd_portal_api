package com.wd.api.estimation.service.calc;

import java.math.BigDecimal;

/**
 * Result of {@link EstimationCalculator#calculateBudgetary} — area × baseRate ± band, GST applied.
 */
public record BudgetaryBreakdown(
        BigDecimal area,
        BigDecimal baseRatePerSqft,
        BigDecimal midPreGst,
        BigDecimal grandTotalMin,
        BigDecimal grandTotalMax) {}
