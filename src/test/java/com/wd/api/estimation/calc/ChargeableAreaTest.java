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

class ChargeableAreaTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void singleFloor_areaIsLengthTimesWidth() {
        EstimationContext ctx = contextWith(dim(List.of(floor("GF", "35", "30")), "0", "0"));
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("1050");
    }

    @Test
    void multipleFloors_areaIsSum() {
        EstimationContext ctx = contextWith(dim(List.of(
                floor("GF", "35", "30"),
                floor("FF", "35", "30")), "0", "0"));
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("2100");
    }

    @Test
    void multipleFloors_differentDimensions_areaIsSum() {
        EstimationContext ctx = contextWith(dim(List.of(
                floor("GF", "40", "30"),
                floor("FF", "35", "30")), "0", "0"));
        // 1200 + 1050 = 2250
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("2250");
    }

    @Test
    void semiCoveredArea_appliesFactor_0_50() {
        EstimationContext ctx = contextWith(dim(List.of(), "200", "0"));
        // 0 builtUp + 200 × 0.50 = 100
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("100.00");
    }

    @Test
    void openTerraceArea_appliesFactor_0_25() {
        EstimationContext ctx = contextWith(dim(List.of(), "0", "400"));
        // 0 builtUp + 400 × 0.25 = 100
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("100.00");
    }

    @Test
    void allThreeAreas_sumsCorrectly() {
        EstimationContext ctx = contextWith(dim(
                List.of(floor("GF", "35", "30")), "200", "400"));
        // 1050 + (200 × 0.50) + (400 × 0.25) = 1050 + 100 + 100 = 1250
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("1250.00");
    }
}
