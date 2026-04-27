package com.wd.api.dto.dpc;

import java.math.BigDecimal;

/**
 * Per-scope cost rollup row.
 *
 * {@code originalAmount} is the BoQ BASE total for items mapped to this scope.
 * {@code customizedAmount} is BASE + ADDON.  {@code variance} is the difference.
 */
public record DpcCostRollupDto(
        String scopeCode,
        String scopeTitle,
        BigDecimal originalAmount,
        BigDecimal customizedAmount,
        BigDecimal variance,
        BigDecimal originalPerSqft,
        BigDecimal customizedPerSqft
) {

    /** True if this scope has any ADDON variance (customized != original). */
    public boolean hasCustomization() {
        if (variance == null) return false;
        return variance.compareTo(BigDecimal.ZERO) != 0;
    }
}
