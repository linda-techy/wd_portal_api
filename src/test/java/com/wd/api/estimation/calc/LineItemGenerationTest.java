package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.LineType;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.PricingMode;
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

class LineItemGenerationTest {

    private final EstimationCalculator calc = new EstimationCalculator();

    @Test
    void minimalQuote_emitsBaseAndGstOnly() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"),
                List.of(), List.of(), List.of(), List.of(), new BigDecimal("1.0000"));
        var lines = calc.calculate(ctx).lineItems();
        assertThat(lines).extracting(li -> li.lineType())
                .containsExactly(LineType.BASE, LineType.GST);
    }

    @Test
    void zeroDiscount_emitsNoDiscountLine() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"),
                List.of(), List.of(), List.of(), List.of(), new BigDecimal("1.0000"));
        assertThat(calc.calculate(ctx).lineItems())
                .noneMatch(li -> li.lineType() == LineType.DISCOUNT);
    }

    @Test
    void zeroGst_emitsNoGstLine() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), new BigDecimal("1.0000"));
        assertThat(calc.calculate(ctx).lineItems())
                .noneMatch(li -> li.lineType() == LineType.GST);
    }

    @Test
    void zeroFluctuation_emitsNoFluctuationLine() {
        EstimationContext ctx = ctx(BigDecimal.ZERO, new BigDecimal("0.18"),
                List.of(), List.of(), List.of(), List.of(), new BigDecimal("1.0000"));
        assertThat(calc.calculate(ctx).lineItems())
                .noneMatch(li -> li.lineType() == LineType.FLUCTUATION);
    }

    @Test
    void allContributionsPresent_emitsOneLinePerContribution_inOrder() {
        CustomisationChoice cust = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Italian Marble", PricingMode.PER_SQFT,
                new BigDecimal("770"), new BigDecimal("1050"));
        SiteFeeApplied site = new SiteFeeApplied(
                UUID.randomUUID(), "Soil", SiteFeeMode.LUMP, new BigDecimal("75000"), null);
        AddOnApplied addOn = new AddOnApplied(UUID.randomUUID(), "Solar", new BigDecimal("180000"));
        GovtFeeApplied fee = new GovtFeeApplied(UUID.randomUUID(), "Permit", new BigDecimal("25000"));
        EstimationContext ctx = ctx(new BigDecimal("0.05"), new BigDecimal("0.18"),
                List.of(cust), List.of(site), List.of(addOn), List.of(fee),
                new BigDecimal("1.0400"));
        var lines = calc.calculate(ctx).lineItems();
        assertThat(lines).extracting(li -> li.lineType()).containsExactly(
                LineType.BASE,
                LineType.CUSTOMISATION,
                LineType.SITE,
                LineType.ADDON,
                LineType.FLUCTUATION,
                LineType.FEE,
                LineType.DISCOUNT,
                LineType.GST);
        // displayOrder is 1..N in calculation order
        for (int i = 0; i < lines.size(); i++) {
            assertThat(lines.get(i).displayOrder()).isEqualTo(i + 1);
        }
    }

    @Test
    void discountLineItem_amountIsNegative() {
        // Discount lines store amount as negative (the calculator emits discount.negate()
        // so a downstream PDF/UI renderer can sum line-item amounts and get the right total).
        EstimationContext ctx = ctx(new BigDecimal("0.05"), new BigDecimal("0.18"),
                List.of(), List.of(), List.of(), List.of(), new BigDecimal("1.0000"));
        var discountLine = calc.calculate(ctx).lineItems().stream()
                .filter(li -> li.lineType() == LineType.DISCOUNT)
                .findFirst()
                .orElseThrow();
        assertThat(discountLine.amount()).isLessThan(BigDecimal.ZERO);
    }

    private EstimationContext ctx(
            BigDecimal discountPercent, BigDecimal gstRate,
            List<CustomisationChoice> customs, List<SiteFeeApplied> sites,
            List<AddOnApplied> addOns, List<GovtFeeApplied> govt,
            BigDecimal compositeIndex) {
        return new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        compositeIndex, Map.of()),
                dim(List.of(floor("GF", "35", "30")), "0", "0"),
                customs, sites, addOns, govt,
                discountPercent, gstRate,
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
