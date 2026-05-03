package com.wd.api.estimation.service.calc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EstimationCalculatorBudgetaryTest {

    @Test
    void budgetary_returns_band_around_area_times_baseRate_with_gst() {
        // 2000 sqft × ₹2000 baseRate = ₹4,000,000 mid
        // ±10% → low ₹3,600,000, high ₹4,400,000
        // GST 18% → low_with_gst ₹4,248,000, high_with_gst ₹5,192,000
        BudgetaryBreakdown b = new EstimationCalculator().calculateBudgetary(
                new BigDecimal("2000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("0.10"),
                new BigDecimal("0.18"));

        assertThat(b.midPreGst()).isEqualByComparingTo("4000000.00");
        assertThat(b.grandTotalMin()).isEqualByComparingTo("4248000.00");
        assertThat(b.grandTotalMax()).isEqualByComparingTo("5192000.00");
    }

    @Test
    void budgetary_zero_band_collapses_to_single_value() {
        BudgetaryBreakdown b = new EstimationCalculator().calculateBudgetary(
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                new BigDecimal("0.18"));

        assertThat(b.grandTotalMin()).isEqualByComparingTo(b.grandTotalMax());
        assertThat(b.grandTotalMin()).isEqualByComparingTo("1770000.00");
    }

    @Test
    void budgetary_rejects_non_positive_area() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new EstimationCalculator().calculateBudgetary(
                        BigDecimal.ZERO,
                        new BigDecimal("2000.00"),
                        new BigDecimal("0.10"),
                        new BigDecimal("0.18")));
    }
}
