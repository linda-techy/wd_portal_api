package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.EstimationCalculator;
import com.wd.api.estimation.service.calc.EstimationContext;
import com.wd.api.estimation.service.calc.view.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.wd.api.estimation.calc.TestContexts.dim;
import static com.wd.api.estimation.calc.TestContexts.floor;
import static org.assertj.core.api.Assertions.assertThat;

class PrecisionTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void semiCoveredAtFactorBoundary_chargeableIsExactly100() {
        // 200 × 0.50 = 100.00 exactly, no drift
        EstimationContext ctx = TestContexts.contextWith(dim(List.of(), "200", "0"));
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("100.00");
    }

    @Test
    void terraceAtFactorBoundary_chargeableIsExactly100() {
        // 400 × 0.25 = 100.00 exactly
        EstimationContext ctx = TestContexts.contextWith(dim(List.of(), "0", "400"));
        assertThat(calc.calculate(ctx).chargeableArea()).isEqualByComparingTo("100.00");
    }

    @Test
    void thirdsCustomisation_grandTotalStableAcrossTenRepeats() {
        // 1/3 area: 1050 / 3 = 350 (exact). Use a delta that triggers division: 100/3 = 33.333...
        // Multiplied by 350: should be a stable reproducible number.
        CustomisationChoice c = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Thirds test", PricingMode.PER_SQFT,
                new BigDecimal("100").divide(new BigDecimal("3"), 10, java.math.RoundingMode.HALF_UP),
                new BigDecimal("350"));
        EstimationContext ctx = make(List.of(c));
        BigDecimal first = calc.calculate(ctx).grandTotal();
        for (int i = 0; i < 9; i++) {
            assertThat(calc.calculate(ctx).grandTotal()).isEqualByComparingTo(first);
        }
    }

    @Test
    void manySmallCustomisations_sumsExactly() {
        // 100 customisations of ₹1.23 × 10 sqft = ₹12.30 each → total ₹1230.00
        java.util.List<CustomisationChoice> many = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            many.add(new CustomisationChoice(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Small " + i, PricingMode.PER_SQFT,
                    new BigDecimal("1.23"), new BigDecimal("10")));
        }
        EstimationContext ctx = make(many);
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("1230.00");
    }

    @Test
    void singleFloor_repeatedCalls_produceIdenticalLineItemAmounts() {
        EstimationContext ctx = TestContexts.contextWith(dim(List.of(floor("GF", "35", "30")), "0", "0"));
        var first = calc.calculate(ctx);
        var second = calc.calculate(ctx);
        assertThat(second.lineItems()).hasSize(first.lineItems().size());
        for (int i = 0; i < first.lineItems().size(); i++) {
            assertThat(second.lineItems().get(i).amount())
                    .isEqualByComparingTo(first.lineItems().get(i).amount());
        }
    }

    private EstimationContext make(List<CustomisationChoice> custs) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                custs, List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
