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
        // Stub — Tasks 4-13 fill in formula stages.
        // Returns a zero breakdown so dispatch tests pass; subsequent tasks replace this body.
        java.math.BigDecimal zero = java.math.BigDecimal.ZERO;
        return new EstimationBreakdown(
                zero, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
                new ArrayList<>(), new ArrayList<>());
    }
}
