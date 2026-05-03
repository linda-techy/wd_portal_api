package com.wd.api.estimation.domain.enums;

/**
 * Top-level estimation pricing mode (distinct from the customisation-level PricingMode).
 *
 * BUDGETARY: Lead stage. (area × baseRatePerSqft) with ±10% band. No floor breakdown.
 * LINE_ITEM: Detailed stage. Per-floor dimensions + customisations + addons + fees.
 */
public enum EstimationPricingMode {
    BUDGETARY,
    LINE_ITEM
}
