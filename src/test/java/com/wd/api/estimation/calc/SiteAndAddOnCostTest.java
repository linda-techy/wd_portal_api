package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.domain.enums.SiteFeeMode;
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

class SiteAndAddOnCostTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void noSiteFees_siteCostIsZero() {
        EstimationContext ctx = build(List.of(), List.of());
        assertThat(calc.calculate(ctx).siteCost()).isEqualByComparingTo("0");
    }

    @Test
    void lumpSiteFee_addsExactAmount() {
        SiteFeeApplied lump = new SiteFeeApplied(
                UUID.randomUUID(), "Soil surcharge", SiteFeeMode.LUMP,
                new BigDecimal("75000.00"), null);
        EstimationContext ctx = build(List.of(lump), List.of());
        assertThat(calc.calculate(ctx).siteCost()).isEqualByComparingTo("75000.00");
    }

    @Test
    void perSqftSiteFee_multipliesByChargeableArea() {
        // 12.00/sqft × 1050 sqft = 12,600
        SiteFeeApplied excavation = new SiteFeeApplied(
                UUID.randomUUID(), "Excavation", SiteFeeMode.PER_SQFT,
                null, new BigDecimal("12.00"));
        EstimationContext ctx = build(List.of(excavation), List.of());
        assertThat(calc.calculate(ctx).siteCost()).isEqualByComparingTo("12600.00");
    }

    @Test
    void mixedSiteFees_sumsBoth() {
        SiteFeeApplied lump = new SiteFeeApplied(
                UUID.randomUUID(), "Soil", SiteFeeMode.LUMP, new BigDecimal("75000"), null);
        SiteFeeApplied perSqft = new SiteFeeApplied(
                UUID.randomUUID(), "Excavation", SiteFeeMode.PER_SQFT,
                null, new BigDecimal("12.00"));
        EstimationContext ctx = build(List.of(lump, perSqft), List.of());
        // 75,000 + 12,600 = 87,600
        assertThat(calc.calculate(ctx).siteCost()).isEqualByComparingTo("87600.00");
    }

    @Test
    void addOns_sumLumpAmounts() {
        AddOnApplied solar = new AddOnApplied(UUID.randomUUID(), "Solar 3kW", new BigDecimal("180000"));
        AddOnApplied lift = new AddOnApplied(UUID.randomUUID(), "Lift", new BigDecimal("450000"));
        EstimationContext ctx = build(List.of(), List.of(solar, lift));
        assertThat(calc.calculate(ctx).addOnCost()).isEqualByComparingTo("630000.00");
    }

    private EstimationContext build(List<SiteFeeApplied> sites, List<AddOnApplied> addOns) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                List.of(), sites, addOns, List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
