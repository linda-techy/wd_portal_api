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

class SubtotalToGrandTotalTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void subtotal_isBaseCustomSiteAddonFluctuation() {
        // base 2,467,500 + custom 0 + site 0 + addon 0 + fluctuation 0 = 2,467,500
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"), List.of());
        assertThat(calc.calculate(ctx).subtotal()).isEqualByComparingTo("2467500.00");
    }

    @Test
    void govtFees_areSummed_andSeparate_fromSubtotal() {
        GovtFeeApplied permit = new GovtFeeApplied(UUID.randomUUID(), "Building permit", new BigDecimal("25000"));
        GovtFeeApplied electricity = new GovtFeeApplied(UUID.randomUUID(), "Electricity", new BigDecimal("15000"));
        EstimationContext ctx = ctxWithGovtFees(List.of(permit, electricity));
        assertThat(calc.calculate(ctx).govtFees()).isEqualByComparingTo("40000.00");
        assertThat(calc.calculate(ctx).subtotal()).isEqualByComparingTo("2467500.00");  // unchanged
    }

    @Test
    void discount_isPercentageOfPreTax() {
        // pre-tax = subtotal + govtFees = 2,467,500 + 0 = 2,467,500
        // 5% discount = 123,375
        EstimationContext ctx = ctx(new BigDecimal("0.05"), new BigDecimal("0.18"), List.of());
        assertThat(calc.calculate(ctx).discount()).isEqualByComparingTo("123375.00");
    }

    @Test
    void taxable_isPreTaxMinusDiscount() {
        EstimationContext ctx = ctx(new BigDecimal("0.05"), new BigDecimal("0.18"), List.of());
        // 2,467,500 - 123,375 = 2,344,125
        assertThat(calc.calculate(ctx).taxable()).isEqualByComparingTo("2344125.00");
    }

    @Test
    void gst_isPercentageOfTaxable() {
        // 18% of 2,467,500 = 444,150 (no discount case)
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"), List.of());
        assertThat(calc.calculate(ctx).gst()).isEqualByComparingTo("444150.00");
    }

    @Test
    void grandTotal_isTaxablePlusGst() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"), List.of());
        // 2,467,500 + 444,150 = 2,911,650
        assertThat(calc.calculate(ctx).grandTotal()).isEqualByComparingTo("2911650.00");
    }

    @Test
    void hand_calculated_smoke_test() {
        // Smoke test from spec §4.5: STANDARD 1050 sqft, no customisation, composite=1.0000, GST 18%
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"), List.of());
        var b = calc.calculate(ctx);
        assertThat(b.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(b.baseCost()).isEqualByComparingTo("2467500.00");
        assertThat(b.fluctuationAdjustment()).isEqualByComparingTo("0.00");
        assertThat(b.gst()).isEqualByComparingTo("444150.00");
        assertThat(b.grandTotal()).isEqualByComparingTo("2911650.00");
    }

    @Test
    void zeroGstRate_taxableEqualsGrandTotal() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        assertThat(calc.calculate(ctx).gst()).isEqualByComparingTo("0");
        assertThat(calc.calculate(ctx).taxable()).isEqualByComparingTo(calc.calculate(ctx).grandTotal());
    }

    private EstimationContext ctx(BigDecimal discountPercent, BigDecimal gstRate, List<GovtFeeApplied> govtFees) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                List.of(), List.of(), List.of(), govtFees,
                discountPercent, gstRate,
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }

    private EstimationContext ctxWithGovtFees(List<GovtFeeApplied> govtFees) {
        return ctx(BigDecimal.ZERO, new BigDecimal("0.18"), govtFees);
    }
}
