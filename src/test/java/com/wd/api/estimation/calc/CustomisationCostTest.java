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

import static com.wd.api.estimation.calc.TestContexts.contextWith;
import static com.wd.api.estimation.calc.TestContexts.dim;
import static com.wd.api.estimation.calc.TestContexts.floor;
import static org.assertj.core.api.Assertions.assertThat;

class CustomisationCostTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void noCustomisations_costIsZero() {
        EstimationContext ctx = contextWith(dim(List.of(floor("GF", "35", "30")), "0", "0"));
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("0");
    }

    @Test
    void singleUpgrade_costIsDeltaTimesArea() {
        // Italian Marble upgrade: delta ₹770/sqft × 1050 sqft = 808,500
        CustomisationChoice flooring = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Italian Marble (vs Vitrified)",
                PricingMode.PER_SQFT,
                new BigDecimal("770.00"),
                new BigDecimal("1050.00"));
        EstimationContext ctx = withCustomisations(List.of(flooring));
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("808500.00");
    }

    @Test
    void downgrade_negativeDelta_subtracts() {
        // Premium customer downgrades flooring to Standard option: -200/sqft × 1050 = -210,000
        CustomisationChoice downgrade = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Vitrified (vs Italian Marble)",
                PricingMode.PER_SQFT,
                new BigDecimal("-200.00"),
                new BigDecimal("1050.00"));
        EstimationContext ctx = withCustomisations(List.of(downgrade));
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("-210000.00");
    }

    @Test
    void multipleCustomisations_sumSignedValues() {
        // Upgrade flooring (+770×1050=808500) + downgrade kitchen (-15000 lump-equivalent stored as delta×1)
        CustomisationChoice up = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Italian Marble", PricingMode.PER_SQFT,
                new BigDecimal("770.00"), new BigDecimal("1050.00"));
        CustomisationChoice down = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Civil kitchen (downgrade from modular)", PricingMode.PER_RFT,
                new BigDecimal("-15000.00"), new BigDecimal("1"));
        EstimationContext ctx = withCustomisations(List.of(up, down));
        // 808,500 + (-15,000) = 793,500
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("793500.00");
    }

    @Test
    void customisationCost_doesNotInflateBaseCost() {
        // base stays 1050 × 2350 = 2,467,500 even with a 808,500 customisation
        CustomisationChoice up = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Italian Marble", PricingMode.PER_SQFT,
                new BigDecimal("770.00"), new BigDecimal("1050.00"));
        EstimationContext ctx = withCustomisations(List.of(up));
        assertThat(calc.calculate(ctx).baseCost()).isEqualByComparingTo("2467500.00");
        assertThat(calc.calculate(ctx).customisationCost()).isEqualByComparingTo("808500.00");
    }

    private EstimationContext withCustomisations(List<CustomisationChoice> customisations) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                customisations,
                List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
