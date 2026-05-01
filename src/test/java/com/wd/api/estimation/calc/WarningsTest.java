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

class WarningsTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void freshIndex_smallCustomisation_noDiscount_noWarnings() {
        EstimationContext ctx = build(LocalDate.now().minusDays(3),
                List.of(), BigDecimal.ZERO);
        assertThat(calc.calculate(ctx).warnings()).isEmpty();
    }

    @Test
    void staleIndex_emits_marketIndexStale() {
        EstimationContext ctx = build(LocalDate.now().minusDays(20),
                List.of(), BigDecimal.ZERO);
        assertThat(calc.calculate(ctx).warnings()).contains("market-index-stale-14-days");
    }

    @Test
    void customisationExceedsTenPercent_emits_drift() {
        // base = 2,467,500. 11% threshold = 246,750. Customisation of 300,000 trips it.
        CustomisationChoice big = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Big upgrade", PricingMode.PER_SQFT,
                new BigDecimal("300000"), new BigDecimal("1"));
        EstimationContext ctx = build(LocalDate.now(), List.of(big), BigDecimal.ZERO);
        assertThat(calc.calculate(ctx).warnings()).contains("customisation-exceeds-10-percent");
    }

    @Test
    void discountAbove5Percent_emits_threshold() {
        EstimationContext ctx = build(LocalDate.now(), List.of(), new BigDecimal("0.08"));
        assertThat(calc.calculate(ctx).warnings()).contains("discount-exceeds-threshold");
    }

    @Test
    void discountAt5PercentExactly_doesNotEmit_threshold() {
        EstimationContext ctx = build(LocalDate.now(), List.of(), new BigDecimal("0.05"));
        assertThat(calc.calculate(ctx).warnings()).doesNotContain("discount-exceeds-threshold");
    }

    private EstimationContext build(LocalDate snapshotDate, List<CustomisationChoice> custs, BigDecimal discountPercent) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), snapshotDate,
                        new BigDecimal("1.0000"), Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                custs, List.of(), List.of(), List.of(),
                discountPercent, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
