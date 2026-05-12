package com.wd.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralised rounding helpers for financial values.
 *
 * Two precisions exist across the codebase:
 *   - <b>internal</b> (NUMERIC(18,6)): used for stage / BOQ ledger amounts so
 *     percentage splits and successive multiplications don't accumulate
 *     rounding error (e.g. retention split, partial execution).
 *   - <b>display</b> (NUMERIC(15,2)): used for tax invoices, GST line items
 *     and anything the customer or auditor sees in INR.
 *
 * Both convert with HALF_UP per Indian commercial convention (rupees rounded
 * to paise; paise rounded conservatively). All financial code MUST use this
 * utility rather than ad-hoc {@code setScale} calls so a single change of
 * rounding mode (e.g. a future move to banker's rounding for GSTR-3B) is a
 * one-line edit.
 */
public final class MoneyMath {

    /** GST / tax-invoice / display precision: 2 decimals (paise). */
    public static final int DISPLAY_SCALE = 2;

    /** Internal ledger precision: 6 decimals — preserves split fidelity. */
    public static final int INTERNAL_SCALE = 6;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private MoneyMath() {}

    /** Round to display precision (2 decimals, HALF_UP). Null-safe. */
    public static BigDecimal roundDisplay(BigDecimal value) {
        return value == null ? null : value.setScale(DISPLAY_SCALE, ROUNDING);
    }

    /** Round to internal-ledger precision (6 decimals, HALF_UP). Null-safe. */
    public static BigDecimal roundInternal(BigDecimal value) {
        return value == null ? null : value.setScale(INTERNAL_SCALE, ROUNDING);
    }

    /**
     * Compute GST for an invoice line: {@code base * (rate / 100)} rounded to
     * display precision. {@code gstRate} is expressed as a percentage
     * (e.g. {@code 18} for 18 %).
     */
    public static BigDecimal gstFromRate(BigDecimal base, BigDecimal gstRate) {
        if (base == null || gstRate == null) {
            return BigDecimal.ZERO.setScale(DISPLAY_SCALE);
        }
        return base.multiply(gstRate)
                .divide(HUNDRED, DISPLAY_SCALE, ROUNDING);
    }

    /**
     * Compute GST for an internal-ledger line (6-decimal precision). Use this
     * when the GST amount feeds a later split (e.g. retention) so successive
     * roundings don't drift; round to display only when the value crosses an
     * invoice boundary.
     */
    public static BigDecimal gstFromRateInternal(BigDecimal base, BigDecimal gstRate) {
        if (base == null || gstRate == null) {
            return BigDecimal.ZERO.setScale(INTERNAL_SCALE);
        }
        return base.multiply(gstRate)
                .divide(HUNDRED, INTERNAL_SCALE, ROUNDING);
    }

    /** Multiply two ledger values and round to internal precision. */
    public static BigDecimal multiplyInternal(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO.setScale(INTERNAL_SCALE);
        }
        return a.multiply(b).setScale(INTERNAL_SCALE, ROUNDING);
    }
}
