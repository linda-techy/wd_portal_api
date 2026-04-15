package com.wd.api.model.enums;

import java.math.BigDecimal;

/**
 * Hierarchical approval levels for Variation Orders.
 * Thresholds (INR, inclusive):
 *   PM                  — approved_cost < 750,000
 *   COMMERCIAL_MANAGER  — 750,000 ≤ approved_cost ≤ 3,750,000
 *   DIRECTOR            — approved_cost > 3,750,000
 */
public enum ApprovalLevel {
    PM,
    COMMERCIAL_MANAGER,
    DIRECTOR;

    private static final BigDecimal PM_MAX          = new BigDecimal("750000");
    private static final BigDecimal CM_MAX          = new BigDecimal("3750000");

    /** Returns the minimum approval level required for the given cost. */
    public static ApprovalLevel requiredFor(BigDecimal approvedCost) {
        if (approvedCost == null) return PM;
        if (approvedCost.compareTo(PM_MAX) < 0) return PM;
        if (approvedCost.compareTo(CM_MAX) <= 0) return COMMERCIAL_MANAGER;
        return DIRECTOR;
    }

    /** Returns true if this level can approve a VO that requires {@code required}. */
    public boolean canApprove(ApprovalLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
