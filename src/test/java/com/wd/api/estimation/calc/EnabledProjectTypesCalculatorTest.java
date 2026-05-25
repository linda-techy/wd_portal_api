package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.EstimationBreakdown;
import com.wd.api.estimation.service.calc.EstimationCalculator;
import com.wd.api.estimation.service.calc.EstimationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for enabling RENOVATION/INTERIOR/COMPOUND via the parametric engine.
 * Verifies both success path (with a configured rate version context) and regression
 * for NEW_BUILD / COMMERCIAL.
 */
class EnabledProjectTypesCalculatorTest {

    private final EstimationCalculator calculator = new EstimationCalculator();

    // --- SUCCESS: RENOVATION/INTERIOR/COMPOUND dispatch through parametric engine ---

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"RENOVATION", "INTERIOR", "COMPOUND"})
    void extendedTypes_withRateVersionContext_returnParametricBreakdown(ProjectType type) {
        EstimationContext ctx = EstimationCalculatorDispatchTest.contextOf(type);

        EstimationBreakdown result = calculator.calculate(ctx);

        assertThat(result).isNotNull();
        // chargeable area = 35 × 30 = 1050 sqft; baseRate = 1500+550+300 = 2350; baseCost = 2467500
        assertThat(result.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(result.baseCost()).isEqualByComparingTo("2467500.00");
        assertThat(result.grandTotal()).isPositive();
        // Must NOT throw UnsupportedProjectTypeException
    }

    // --- REGRESSION: NEW_BUILD / COMMERCIAL behaviour unchanged ---

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"NEW_BUILD", "COMMERCIAL"})
    void newBuildAndCommercial_regression_returnsIdenticalBreakdown(ProjectType type) {
        EstimationContext ctx = EstimationCalculatorDispatchTest.contextOf(type);

        EstimationBreakdown result = calculator.calculate(ctx);

        assertThat(result).isNotNull();
        assertThat(result.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(result.baseCost()).isEqualByComparingTo("2467500.00");
        // GST 18% on 2467500 = 444150; total = 2911650
        assertThat(result.grandTotal()).isEqualByComparingTo("2911650.00");
    }
}
