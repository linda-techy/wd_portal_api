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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void nullMarketIndex_throwsIllegalArgument() {
        EstimationContext ctx = ctx(null, new BigDecimal("1"), new BigDecimal("1"), List.of());
        assertThatThrownBy(() -> calc.calculate(ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("marketIndex");
    }

    @Test
    void emptyFloors_andZeroSemiAndZeroTerrace_throwsIllegalArgument() {
        // Genuinely no chargeable area at all means caller built the context wrong
        EstimationContext ctx = ctx(snapshot(), new BigDecimal("0"), new BigDecimal("0"), List.of());
        EstimationContext withNoFloors = withFloors(ctx, List.of());
        assertThatThrownBy(() -> calc.calculate(withNoFloors))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions");
    }

    @Test
    void negativeApplicableArea_throwsIllegalArgument() {
        CustomisationChoice bad = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Bad fixture", PricingMode.PER_SQFT,
                new BigDecimal("100"), new BigDecimal("-50"));
        EstimationContext ctx = ctx(snapshot(), new BigDecimal("1"), new BigDecimal("1"), List.of(bad));
        assertThatThrownBy(() -> calc.calculate(ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicableArea");
    }

    @Test
    void negativeDimensions_throwsIllegalArgument() {
        EstimationContext ctx = ctx(snapshot(),
                new BigDecimal("-1"), new BigDecimal("1"), List.of());
        assertThatThrownBy(() -> calc.calculate(ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semiCoveredArea");
    }

    private MarketIndexSnapshotView snapshot() {
        return new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.now(),
                new BigDecimal("1.0000"), Map.of());
    }

    private EstimationContext ctx(MarketIndexSnapshotView mi, BigDecimal semi, BigDecimal terrace,
                                   List<CustomisationChoice> custs) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                mi,
                new Dimensions(List.of(new Floor("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        semi, terrace),
                custs, List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }

    private EstimationContext withFloors(EstimationContext base, List<Floor> floors) {
        return new EstimationContext(
                base.projectType(), base.pkg(), base.rateVersion(), base.marketIndex(),
                new Dimensions(floors, base.dimensions().semiCoveredArea(), base.dimensions().openTerraceArea()),
                base.customisations(), base.siteFees(), base.addOns(), base.govtFees(),
                base.discountPercent(), base.gstRate(), base.constants());
    }
}
