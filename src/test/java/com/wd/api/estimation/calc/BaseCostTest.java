package com.wd.api.estimation.calc;

import com.wd.api.estimation.service.calc.EstimationCalculator;
import com.wd.api.estimation.service.calc.EstimationContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.wd.api.estimation.calc.TestContexts.contextWith;
import static com.wd.api.estimation.calc.TestContexts.dim;
import static com.wd.api.estimation.calc.TestContexts.floor;
import static org.assertj.core.api.Assertions.assertThat;

class BaseCostTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void standardPackage_1050sqft_baseCostIs2467500() {
        // 1050 sqft × (1500 + 550 + 300) = 1050 × 2350 = 2,467,500
        EstimationContext ctx = contextWith(dim(List.of(floor("GF", "35", "30")), "0", "0"));
        assertThat(calc.calculate(ctx).baseCost()).isEqualByComparingTo("2467500.00");
    }

    @Test
    void multiFloor_baseCostScalesLinearly() {
        // 2100 sqft × 2350 = 4,935,000
        EstimationContext ctx = contextWith(dim(List.of(
                floor("GF", "35", "30"),
                floor("FF", "35", "30")), "0", "0"));
        assertThat(calc.calculate(ctx).baseCost()).isEqualByComparingTo("4935000.00");
    }
}
