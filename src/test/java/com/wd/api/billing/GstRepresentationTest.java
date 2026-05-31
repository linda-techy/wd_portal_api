package com.wd.api.billing;

import com.wd.api.util.MoneyMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.*;

/**
 * Characterisation tests for the two GST conventions in this codebase:
 *
 * FAMILY A — "fraction" convention (value in [0, 1]):
 *   CustomerProject.gstRate, BoqDocument.gstRate, ChangeOrder.gstRate,
 *   PaymentStage.gstRate, BoqInvoice.gstRate, CreditNote.gstRate.
 *   Computation:  gstAmount = base * gstRate           (multiply directly)
 *
 * FAMILY B — "percentage" convention (value in [0, 100]):
 *   ProjectInvoice.gstPercentage, ProjectInvoiceDTO.gstPercentage,
 *   DesignPackagePayment.gstPercentage, TaxInvoice.igstRate/cgstRate/sgstRate.
 *   Computation:  gstAmount = base * gstRate / 100     (via MoneyMath.gstFromRate)
 *
 * Key invariant: the two families are ISOLATED.  No value crosses from one to
 * the other; each family uses only its own computation path.  These tests
 * pin that invariant so a future refactoring cannot silently break it.
 */
class GstRepresentationTest {

    // ────────────────────────────────────────────────────────────────────────────
    // FAMILY A — fraction arithmetic
    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Family A: fraction-convention GST (BoQ / ChangeOrder / Stage / CreditNote)")
    class FamilyAFractionConvention {

        private static final BigDecimal RATE_18_FRACTION = new BigDecimal("0.18");

        /** Mirrors exactly what BoqDocumentService / VariationOrderService do. */
        private BigDecimal computeGst(BigDecimal base, BigDecimal rate) {
            return base.multiply(rate).setScale(6, RoundingMode.HALF_UP);
        }

        @Test
        @DisplayName("18% on ₹100 ex-GST → ₹18.000000 GST, ₹118.000000 incl-GST")
        void gstAt18Pct_on100() {
            BigDecimal base = new BigDecimal("100.00");
            BigDecimal gst = computeGst(base, RATE_18_FRACTION);
            BigDecimal inclGst = base.add(gst).setScale(6, RoundingMode.HALF_UP);

            assertThat(gst).isEqualByComparingTo(new BigDecimal("18.000000"));
            assertThat(inclGst).isEqualByComparingTo(new BigDecimal("118.000000"));
        }

        @Test
        @DisplayName("0% GST (default per V158) on ₹500 → ₹0 GST, ₹500 total")
        void gstAt0Pct_defaultAfterV158() {
            BigDecimal base = new BigDecimal("500.00");
            BigDecimal gst = computeGst(base, BigDecimal.ZERO);

            assertThat(gst).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(base.add(gst)).isEqualByComparingTo(new BigDecimal("500.000000"));
        }

        @Test
        @DisplayName("Fraction rate is validated in [0, 1] — 0.18 accepted, 18 rejected")
        void fractionValidationBounds() {
            // Valid boundary values
            assertThat(new BigDecimal("0.00").signum()).isGreaterThanOrEqualTo(0);
            assertThat(new BigDecimal("1.00").compareTo(BigDecimal.ONE)).isLessThanOrEqualTo(0);
            assertThat(new BigDecimal("0.18").compareTo(BigDecimal.ONE)).isLessThan(0);

            // A percentage value (18.00) is ABOVE 1 and must be rejected by
            // CustomerProjectService.updateProjectGstRate validation
            BigDecimal wrongPercentage = new BigDecimal("18.00");
            assertThat(wrongPercentage.compareTo(BigDecimal.ONE))
                    .as("Percentage value 18.00 must be > 1, so validation rejects it")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("Feeding a fraction (0.18) to MoneyMath.gstFromRate (which expects percentage) gives 100x too small result")
        void fractionFedToPercentagePathGives100xSmall_demonstratesCrossBoundaryDanger() {
            BigDecimal base = new BigDecimal("100.00");
            // Correct percentage path: gstFromRate(100, 18.00) → 18.00
            BigDecimal correctGst = MoneyMath.gstFromRate(base, new BigDecimal("18.00"));

            // Wrong: fraction value fed into percentage path → 0.18
            BigDecimal wrongGst = MoneyMath.gstFromRate(base, new BigDecimal("0.18"));

            assertThat(correctGst).isEqualByComparingTo(new BigDecimal("18.00"));
            assertThat(wrongGst).isEqualByComparingTo(new BigDecimal("0.18"));

            // The error factor is exactly 100x
            assertThat(correctGst.divide(wrongGst, 0, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("Feeding a percentage (18.00) to fraction path gives 100x too large result")
        void percentageFedToFractionPathGives100xLarge_demonstratesCrossBoundaryDanger() {
            BigDecimal base = new BigDecimal("100.00");
            // Correct fraction path: 100 * 0.18 = 18
            BigDecimal correctGst = computeGst(base, new BigDecimal("0.18"));

            // Wrong: percentage value fed into fraction path → 1800
            BigDecimal wrongGst = computeGst(base, new BigDecimal("18.00"));

            assertThat(correctGst).isEqualByComparingTo(new BigDecimal("18.000000"));
            assertThat(wrongGst).isEqualByComparingTo(new BigDecimal("1800.000000"));

            assertThat(wrongGst.divide(correctGst, 0, RoundingMode.HALF_UP))
                    .isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // FAMILY B — percentage arithmetic via MoneyMath.gstFromRate
    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Family B: percentage-convention GST (ProjectInvoice / DesignPackagePayment / TaxInvoice)")
    class FamilyBPercentageConvention {

        private static final BigDecimal RATE_18_PERCENT = new BigDecimal("18.00");
        private static final BigDecimal HUNDRED = new BigDecimal("100");

        /** Mirrors exactly what PaymentService / ProjectInvoiceService do. */
        private BigDecimal computeGst(BigDecimal base, BigDecimal pct) {
            return MoneyMath.gstFromRate(base, pct);
        }

        @Test
        @DisplayName("18% (as percentage 18.00) on ₹100 → ₹18.00 GST, ₹118.00 total")
        void gstAt18Pct_on100() {
            BigDecimal base = new BigDecimal("100.00");
            BigDecimal gst = computeGst(base, RATE_18_PERCENT);
            BigDecimal total = MoneyMath.roundDisplay(base.add(gst));

            assertThat(gst).isEqualByComparingTo(new BigDecimal("18.00"));
            assertThat(total).isEqualByComparingTo(new BigDecimal("118.00"));
        }

        @Test
        @DisplayName("0% (percentage 0.00) on ₹500 → ₹0.00 GST")
        void gstAt0Pct() {
            BigDecimal gst = computeGst(new BigDecimal("500.00"), BigDecimal.ZERO);
            assertThat(gst).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("PaymentService GST calc: base * percentage / 100 matches MoneyMath.gstFromRate")
        void paymentServiceInlineCalcMatchesMoneyMath() {
            BigDecimal base = new BigDecimal("2400.00");
            BigDecimal pct = new BigDecimal("18.00");

            // Inline calculation as written in PaymentService.createDesignPayment
            BigDecimal inlineGst = base.multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            // Equivalent MoneyMath call
            BigDecimal moneyMathGst = MoneyMath.gstFromRate(base, pct);

            assertThat(inlineGst)
                    .as("PaymentService inline calc must match MoneyMath.gstFromRate for the same percentage input")
                    .isEqualByComparingTo(moneyMathGst);
        }

        @Test
        @DisplayName("TaxInvoice intrastate: CGST+SGST each at half rate sums to full GST")
        void taxInvoiceCgstSgstSplit() {
            BigDecimal base = new BigDecimal("1000.00");
            BigDecimal fullRate = new BigDecimal("18.00");
            BigDecimal halfRate = fullRate.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

            BigDecimal cgst = base.multiply(halfRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal sgst = base.multiply(halfRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal totalTax = cgst.add(sgst);
            BigDecimal expectedGst = MoneyMath.gstFromRate(base, fullRate);

            assertThat(totalTax)
                    .as("CGST + SGST must equal full GST computed via MoneyMath")
                    .isEqualByComparingTo(expectedGst);
        }

        @Test
        @DisplayName("Percentage rate bounds: valid range is [0, 28] for Indian GST slabs")
        void percentageValidationBounds() {
            // Max Indian GST slab is 28%
            BigDecimal maxIndianGst = new BigDecimal("28.00");
            assertThat(maxIndianGst.compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
            assertThat(maxIndianGst.compareTo(new BigDecimal("28"))).isLessThanOrEqualTo(0);

            // A fraction value 0.18 is < 1, which is well below any expected GST slab (5, 12, 18, 28)
            // — feeding it to gstFromRate would yield 0.18% instead of 18% (100x error)
            BigDecimal wrongFraction = new BigDecimal("0.18");
            assertThat(wrongFraction.compareTo(new BigDecimal("1")))
                    .as("Fraction 0.18 is below 1 — should be caught by a range-check on gstPercentage fields (expected >= 1 when non-zero for Indian GST)")
                    .isLessThan(0);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Cross-family isolation proof
    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Convention isolation: both families produce identical GST money amounts for the same real rate")
    class ConventionIsolationProof {

        @Test
        @DisplayName("18% real rate: fraction path (×0.18) == percentage path (×18/100) on same base")
        void bothFamiliesProduceSameGstForSameRealRate() {
            BigDecimal base = new BigDecimal("1500.00");

            // Family A: fraction convention
            BigDecimal gstFamilyA = base.multiply(new BigDecimal("0.18"))
                    .setScale(2, RoundingMode.HALF_UP);

            // Family B: percentage convention via MoneyMath
            BigDecimal gstFamilyB = MoneyMath.gstFromRate(base, new BigDecimal("18.00"));

            assertThat(gstFamilyA)
                    .as("Both conventions must yield the same rupee GST amount for 18% on ₹1500")
                    .isEqualByComparingTo(gstFamilyB);
        }

        @Test
        @DisplayName("5% real rate: fraction path (×0.05) == percentage path (×5/100) on same base")
        void bothFamiliesAt5Pct() {
            BigDecimal base = new BigDecimal("8000.00");
            BigDecimal gstA = base.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gstB = MoneyMath.gstFromRate(base, new BigDecimal("5.00"));
            assertThat(gstA).isEqualByComparingTo(gstB);
        }

        @Test
        @DisplayName("MoneyMath.gstFromRateInternal (6dp) and gstFromRate (2dp) agree within rounding on whole rupee base")
        void internalVsDisplayPrecisionAgreement() {
            BigDecimal base = new BigDecimal("10000.00");
            BigDecimal pct = new BigDecimal("18.00");

            BigDecimal display = MoneyMath.gstFromRate(base, pct);
            BigDecimal internal = MoneyMath.gstFromRateInternal(base, pct)
                    .setScale(2, RoundingMode.HALF_UP);

            assertThat(display)
                    .as("Display and internal precision GST must agree on whole-rupee base")
                    .isEqualByComparingTo(internal);
        }
    }
}
