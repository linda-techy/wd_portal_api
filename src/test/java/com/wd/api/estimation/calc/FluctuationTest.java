package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.PackageInternalName;
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

class FluctuationTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void compositeOne_fluctuationIsZero() {
        EstimationContext ctx = ctxAt(new BigDecimal("1.0000"));
        assertThat(calc.calculate(ctx).fluctuationAdjustment()).isEqualByComparingTo("0.00");
    }

    @Test
    void composite_one_oh_four_appliesOnlyToMaterial() {
        // base = 1050 × (1500 + 550 + 300) = 2,467,500
        // material portion = 2,467,500 × 1500/2350 = 1,575,000
        // fluctuation = 1,575,000 × (1.04 - 1) = 63,000
        EstimationContext ctx = ctxAt(new BigDecimal("1.0400"));
        assertThat(calc.calculate(ctx).fluctuationAdjustment()).isEqualByComparingTo("63000.00");
    }

    @Test
    void composite_one_oh_four_baseCost_unchanged() {
        // The fluctuation overlay does NOT modify baseCost itself (it's a separate field)
        EstimationContext ctx = ctxAt(new BigDecimal("1.0400"));
        assertThat(calc.calculate(ctx).baseCost()).isEqualByComparingTo("2467500.00");
    }

    @Test
    void composite_below_one_producesNegativeFluctuation() {
        // 0.96 → material × (0.96 - 1) = -0.04 × 1,575,000 = -63,000
        EstimationContext ctx = ctxAt(new BigDecimal("0.9600"));
        assertThat(calc.calculate(ctx).fluctuationAdjustment()).isEqualByComparingTo("-63000.00");
    }

    @Test
    void labourPlusOverheadPortion_isUnchangedByCompositeIndex() {
        // labour + overhead = 850 × 1050 = 892,500. Verify by checking that
        // fluctuationAdjustment with composite=1.04 only touches material.
        // baseCost remains 2,467,500 regardless. The proof: fluctuation == material × 0.04.
        EstimationContext ctx = ctxAt(new BigDecimal("1.0400"));
        EstimationContext ctxBaseline = ctxAt(new BigDecimal("1.0000"));
        BigDecimal materialOnlyDiff = calc.calculate(ctx).fluctuationAdjustment()
                .subtract(calc.calculate(ctxBaseline).fluctuationAdjustment());
        // Should equal material × 0.04 exactly
        // material = 1,050 × 1,500 = 1,575,000 → × 0.04 = 63,000
        assertThat(materialOnlyDiff).isEqualByComparingTo("63000.00");
    }

    private EstimationContext ctxAt(BigDecimal compositeIndex) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        compositeIndex, Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
