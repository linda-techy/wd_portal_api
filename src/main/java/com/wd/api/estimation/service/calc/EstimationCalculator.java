package com.wd.api.estimation.service.calc;

import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import com.wd.api.estimation.service.calc.view.AddOnApplied;
import com.wd.api.estimation.service.calc.view.CustomisationChoice;
import com.wd.api.estimation.service.calc.view.GovtFeeApplied;
import com.wd.api.estimation.service.calc.view.SiteFeeApplied;

import java.math.MathContext;
import java.math.RoundingMode;

public final class EstimationCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final java.math.BigDecimal DISCOUNT_THRESHOLD = new java.math.BigDecimal("0.05");
    private static final long INDEX_STALE_DAYS = 14;

    public EstimationBreakdown calculate(EstimationContext ctx) {
        return switch (ctx.projectType()) {
            case NEW_BUILD, COMMERCIAL -> calculateNewBuild(ctx);
            case RENOVATION, INTERIOR, COMPOUND -> throw new UnsupportedProjectTypeException(ctx.projectType());
        };
    }

    /**
     * Budgetary mode (K). area × baseRatePerSqft ± bandPercent, then GST applied to both edges.
     * Independent of EstimationContext (no floor breakdown, no customisations, no fees).
     */
    public BudgetaryBreakdown calculateBudgetary(
            java.math.BigDecimal area,
            java.math.BigDecimal baseRatePerSqft,
            java.math.BigDecimal bandPercent,
            java.math.BigDecimal gstRate) {
        if (area == null || area.signum() <= 0) {
            throw new IllegalArgumentException("estimatedAreaSqft must be > 0");
        }
        if (baseRatePerSqft == null || baseRatePerSqft.signum() <= 0) {
            throw new IllegalArgumentException("baseRatePerSqft must be > 0");
        }
        if (bandPercent == null || bandPercent.signum() < 0) {
            throw new IllegalArgumentException("bandPercent must be >= 0");
        }
        if (gstRate == null || gstRate.signum() < 0) {
            throw new IllegalArgumentException("gstRate must be >= 0");
        }
        java.math.BigDecimal mid = area.multiply(baseRatePerSqft, MC);
        java.math.BigDecimal one = java.math.BigDecimal.ONE;
        java.math.BigDecimal lowPreGst = mid.multiply(one.subtract(bandPercent, MC), MC);
        java.math.BigDecimal highPreGst = mid.multiply(one.add(bandPercent, MC), MC);
        java.math.BigDecimal gstMul = one.add(gstRate, MC);
        java.math.BigDecimal low = lowPreGst.multiply(gstMul, MC).setScale(2, RoundingMode.HALF_UP);
        java.math.BigDecimal high = highPreGst.multiply(gstMul, MC).setScale(2, RoundingMode.HALF_UP);
        return new BudgetaryBreakdown(
                area, baseRatePerSqft,
                mid.setScale(2, RoundingMode.HALF_UP),
                low, high);
    }

    private EstimationBreakdown calculateNewBuild(EstimationContext ctx) {
        // Validation
        if (ctx.marketIndex() == null) {
            throw new IllegalArgumentException("marketIndex must not be null");
        }
        if (ctx.dimensions().semiCoveredArea().signum() < 0) {
            throw new IllegalArgumentException("semiCoveredArea must not be negative");
        }
        if (ctx.dimensions().openTerraceArea().signum() < 0) {
            throw new IllegalArgumentException("openTerraceArea must not be negative");
        }
        for (CustomisationChoice c : ctx.customisations()) {
            if (c.applicableArea().signum() < 0) {
                throw new IllegalArgumentException("customisation applicableArea must not be negative: " + c.description());
            }
        }
        if (ctx.dimensions().floors().isEmpty()
                && ctx.dimensions().semiCoveredArea().signum() == 0
                && ctx.dimensions().openTerraceArea().signum() == 0) {
            throw new IllegalArgumentException("dimensions must include at least one floor or non-zero area");
        }

        java.math.BigDecimal zero = java.math.BigDecimal.ZERO;

        // Step 1: chargeable area
        java.math.BigDecimal builtUp = ctx.dimensions().floors().stream()
                .map(f -> f.length().multiply(f.width(), MC))
                .reduce(zero, (a, b) -> a.add(b, MC));
        java.math.BigDecimal semi = ctx.dimensions().semiCoveredArea()
                .multiply(ctx.constants().semiCoveredFactor(), MC);
        java.math.BigDecimal terrace = ctx.dimensions().openTerraceArea()
                .multiply(ctx.constants().openTerraceFactor(), MC);
        java.math.BigDecimal chargeableArea = builtUp.add(semi, MC).add(terrace, MC);

        // Step 2: base package cost (uses pinned rate version)
        java.math.BigDecimal baseRate = ctx.rateVersion().materialRate()
                .add(ctx.rateVersion().labourRate(), MC)
                .add(ctx.rateVersion().overheadRate(), MC);
        java.math.BigDecimal baseCost = chargeableArea.multiply(baseRate, MC);

        // Step 3: customisation deltas (signed; downgrades subtract)
        java.math.BigDecimal customisationCost = ctx.customisations().stream()
                .map(c -> c.deltaRate().multiply(c.applicableArea(), MC))
                .reduce(zero, (a, b) -> a.add(b, MC));

        // Step 4: site work (LUMP or PER_SQFT — XOR enforced at DB level)
        java.math.BigDecimal siteCost = ctx.siteFees().stream()
                .map(f -> f.mode() == com.wd.api.estimation.domain.enums.SiteFeeMode.LUMP
                        ? f.lumpAmount()
                        : f.perSqftRate().multiply(chargeableArea, MC))
                .reduce(zero, (a, b) -> a.add(b, MC));

        // Step 5: add-ons (always lump)
        java.math.BigDecimal addOnCost = ctx.addOns().stream()
                .map(AddOnApplied::lumpAmount)
                .reduce(zero, (a, b) -> a.add(b, MC));

        // Step 6: market fluctuation overlay (ONLY on material portion)
        java.math.BigDecimal materialPortion = baseCost
                .multiply(ctx.rateVersion().materialRate(), MC)
                .divide(baseRate, 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal fluctuationAdjustment = materialPortion
                .multiply(ctx.marketIndex().compositeIndex().subtract(java.math.BigDecimal.ONE, MC), MC);

        // Step 7: subtotal, fees, discount, GST
        java.math.BigDecimal subtotal = baseCost.add(customisationCost, MC)
                .add(siteCost, MC).add(addOnCost, MC).add(fluctuationAdjustment, MC);
        java.math.BigDecimal govtFees = ctx.govtFees().stream()
                .map(GovtFeeApplied::lumpAmount)
                .reduce(zero, (a, b) -> a.add(b, MC));
        java.math.BigDecimal preTax = subtotal.add(govtFees, MC);
        java.math.BigDecimal discount = preTax.multiply(ctx.discountPercent(), MC);
        java.math.BigDecimal taxable = preTax.subtract(discount, MC);
        java.math.BigDecimal gst = taxable.multiply(ctx.gstRate(), MC);
        java.math.BigDecimal grandTotal = taxable.add(gst, MC);

        java.util.List<LineItem> lineItems = new java.util.ArrayList<>();
        int order = 1;
        // BASE always emitted
        lineItems.add(new LineItem(
                com.wd.api.estimation.domain.enums.LineType.BASE,
                "Base package cost (" + ctx.pkg().marketingName() + ", "
                        + chargeableArea.stripTrailingZeros().toPlainString() + " sqft)",
                ctx.pkg().id(),
                chargeableArea, "sqft", baseRate, baseCost, order++));
        // One CUSTOMISATION per choice
        for (CustomisationChoice c : ctx.customisations()) {
            java.math.BigDecimal amt = c.deltaRate().multiply(c.applicableArea(), MC);
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.CUSTOMISATION,
                    c.description(), c.optionId(), c.applicableArea(),
                    pricingUnit(c.pricingMode()), c.deltaRate(), amt, order++));
        }
        // One SITE per fee (mode-specific amount)
        for (SiteFeeApplied f : ctx.siteFees()) {
            java.math.BigDecimal amt = f.mode() == com.wd.api.estimation.domain.enums.SiteFeeMode.LUMP
                    ? f.lumpAmount()
                    : f.perSqftRate().multiply(chargeableArea, MC);
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.SITE,
                    f.name(), f.id(),
                    f.mode() == com.wd.api.estimation.domain.enums.SiteFeeMode.LUMP ? null : chargeableArea,
                    f.mode() == com.wd.api.estimation.domain.enums.SiteFeeMode.LUMP ? "lump" : "sqft",
                    f.mode() == com.wd.api.estimation.domain.enums.SiteFeeMode.LUMP ? null : f.perSqftRate(),
                    amt, order++));
        }
        // One ADDON per add-on
        for (AddOnApplied a : ctx.addOns()) {
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.ADDON,
                    a.name(), a.id(), null, "lump", null, a.lumpAmount(), order++));
        }
        // FLUCTUATION only if non-zero
        if (fluctuationAdjustment.compareTo(zero) != 0) {
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.FLUCTUATION,
                    "Material price fluctuation (composite "
                            + ctx.marketIndex().compositeIndex().toPlainString() + ")",
                    ctx.marketIndex().id(), null, null, null, fluctuationAdjustment, order++));
        }
        // One FEE per govt fee
        for (GovtFeeApplied f : ctx.govtFees()) {
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.FEE,
                    f.name(), f.id(), null, "lump", null, f.lumpAmount(), order++));
        }
        // DISCOUNT only if > 0
        if (discount.compareTo(zero) > 0) {
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.DISCOUNT,
                    "Discount (" + ctx.discountPercent().multiply(new java.math.BigDecimal("100")).toPlainString() + "%)",
                    null, null, null, null, discount.negate(), order++));
        }
        // GST only if > 0
        if (gst.compareTo(zero) > 0) {
            lineItems.add(new LineItem(
                    com.wd.api.estimation.domain.enums.LineType.GST,
                    "GST (" + ctx.gstRate().multiply(new java.math.BigDecimal("100")).toPlainString() + "%)",
                    null, null, null, null, gst, order++));
        }

        java.util.List<String> warnings = new java.util.ArrayList<>();
        long indexAgeDays = java.time.temporal.ChronoUnit.DAYS.between(
                ctx.marketIndex().snapshotDate(), java.time.LocalDate.now());
        if (indexAgeDays > INDEX_STALE_DAYS) {
            warnings.add("market-index-stale-14-days");
        }
        if (baseCost.compareTo(zero) > 0) {
            java.math.BigDecimal driftRatio = customisationCost.abs(MC)
                    .divide(baseCost, 4, java.math.RoundingMode.HALF_UP);
            if (driftRatio.compareTo(ctx.constants().customisationDriftThreshold()) > 0) {
                warnings.add("customisation-exceeds-10-percent");
            }
        }
        if (ctx.discountPercent().compareTo(DISCOUNT_THRESHOLD) > 0) {
            warnings.add("discount-exceeds-threshold");
        }

        return new EstimationBreakdown(
                chargeableArea, baseCost, customisationCost, siteCost, addOnCost,
                fluctuationAdjustment,
                subtotal, govtFees, discount, taxable, gst, grandTotal,
                lineItems, warnings);
    }

    private static String pricingUnit(com.wd.api.estimation.domain.enums.PricingMode mode) {
        return switch (mode) {
            case PER_SQFT -> "sqft";
            case PER_UNIT -> "unit";
            case PER_RFT  -> "rft";
        };
    }
}
