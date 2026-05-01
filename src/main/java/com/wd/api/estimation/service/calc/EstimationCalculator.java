package com.wd.api.estimation.service.calc;

import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;

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

        return new EstimationBreakdown(
                chargeableArea, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
                new ArrayList<>(), new ArrayList<>());
    }
}
