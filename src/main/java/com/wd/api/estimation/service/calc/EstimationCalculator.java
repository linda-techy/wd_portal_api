package com.wd.api.estimation.service.calc;

import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import com.wd.api.estimation.service.calc.view.AddOnApplied;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class EstimationCalculator {

    static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    public EstimationBreakdown calculate(EstimationContext ctx) {
        return switch (ctx.projectType()) {
            case NEW_BUILD, COMMERCIAL -> calculateNewBuild(ctx);
            case RENOVATION, INTERIOR, COMPOUND -> throw new UnsupportedProjectTypeException(ctx.projectType());
        };
    }

    private EstimationBreakdown calculateNewBuild(EstimationContext ctx) {
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

        return new EstimationBreakdown(
                chargeableArea, baseCost, customisationCost, siteCost, addOnCost,
                zero, zero, zero, zero, zero, zero, zero,
                new ArrayList<>(), new ArrayList<>());
    }
}
