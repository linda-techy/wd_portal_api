package com.wd.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralised rounding helpers for financial values.
 *
 * <h3>Two precisions</h3>
 * <ul>
 *   <li><b>internal</b> (NUMERIC(18,6)): used for stage / BOQ ledger amounts so
 *       percentage splits and successive multiplications don't accumulate
 *       rounding error (e.g. retention split, partial execution).</li>
 *   <li><b>display</b> (NUMERIC(15,2)): used for tax invoices, GST line items
 *       and anything the customer or auditor sees in INR.</li>
 * </ul>
 * Both convert with HALF_UP per Indian commercial convention.  All financial
 * code MUST use this utility rather than ad-hoc {@code setScale} calls.
 *
 * <h3>Two GST conventions in this codebase — do NOT mix them</h3>
 * <p>There are exactly two, isolated families:
 * <ul>
 *   <li><b>Family A — fraction convention</b> (value in [0, 1], e.g. {@code 0.18} for 18 %).
 *       Used by: {@code CustomerProject.gstRate}, {@code BoqDocument.gstRate},
 *       {@code ChangeOrder.gstRate}, {@code PaymentStage.gstRate},
 *       {@code BoqInvoice.gstRate}, {@code CreditNote.gstRate}.
 *       Computation: {@code gstAmount = base × rate} (multiply directly — no divide by 100).
 *       Validation: must be in [0, 1]; enforced by
 *       {@code CustomerProjectService.updateProjectGstRate}.
 *   </li>
 *   <li><b>Family B — percentage convention</b> (value in [0, 100], e.g. {@code 18.00} for 18 %).
 *       Used by: {@code ProjectInvoice.gstPercentage},
 *       {@code DesignPackagePayment.gstPercentage}, {@code TaxInvoice.igstRate/cgstRate/sgstRate}.
 *       Computation: {@code gstAmount = base × rate / 100} via {@link #gstFromRate}.
 *       Validation: must be in [0, 28] (highest Indian GST slab).
 *   </li>
 * </ul>
 * <p><b>Cross-contamination is billing-critical:</b> feeding a fraction (0.18)
 * to the percentage path yields GST 100× too small; feeding a percentage (18)
 * to the fraction path yields GST 100× too large.  The two families must
 * never share a value directly.  See {@code GstRepresentationTest} for
 * characterisation tests that pin this invariant.
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
     * Compute GST for an invoice line (Family B — percentage convention):
     * {@code base × (gstRate / 100)} rounded to display precision (2 dp).
     *
     * <p>{@code gstRate} MUST be a <em>percentage</em> value such as {@code 18.00}
     * for 18 %.  Valid Indian GST slabs: 0, 5, 12, 18, 28.  Passing a
     * <em>fraction</em> (e.g. {@code 0.18}) is a billing error that computes
     * GST 100× too small — see class-level Javadoc for the convention map.
     *
     * <p>Callers: {@code ProjectInvoiceService}, {@code PaymentService}
     * (inline calc equivalent), {@code TaxInvoice} computation in
     * {@code PaymentService.generateGstInvoice}.
     */
    public static BigDecimal gstFromRate(BigDecimal base, BigDecimal gstRate) {
        if (base == null || gstRate == null) {
            return BigDecimal.ZERO.setScale(DISPLAY_SCALE);
        }
        return base.multiply(gstRate)
                .divide(HUNDRED, DISPLAY_SCALE, ROUNDING);
    }

    /**
     * Compute GST for an internal-ledger line (Family B — percentage convention,
     * 6-decimal precision). {@code gstRate} MUST be a percentage (e.g. {@code 18.00}).
     *
     * <p>Use this when the GST amount feeds a later split (e.g. retention) so
     * successive roundings don't drift; round to display only when the value
     * crosses an invoice boundary.
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
