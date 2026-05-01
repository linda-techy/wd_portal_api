package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.EstimationContext;
import com.wd.api.estimation.service.calc.view.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shared fixtures so each calculator test can stay short. */
final class TestContexts {

    private TestContexts() {}

    static Floor floor(String name, String length, String width) {
        return new Floor(name, new BigDecimal(length), new BigDecimal(width));
    }

    static Dimensions dim(List<Floor> floors, String semiCovered, String openTerrace) {
        return new Dimensions(floors, new BigDecimal(semiCovered), new BigDecimal(openTerrace));
    }

    static EstimationContext contextWith(Dimensions dimensions) {
        return baseContext(dimensions, ProjectType.NEW_BUILD,
                new BigDecimal("1.0000"),
                BigDecimal.ZERO, new BigDecimal("0.18"));
    }

    static EstimationContext contextWith(Dimensions dimensions, BigDecimal compositeIndex) {
        return baseContext(dimensions, ProjectType.NEW_BUILD, compositeIndex,
                BigDecimal.ZERO, new BigDecimal("0.18"));
    }

    static EstimationContext baseContext(
            Dimensions dimensions,
            ProjectType type,
            BigDecimal compositeIndex,
            BigDecimal discountPercent,
            BigDecimal gstRate) {
        return new EstimationContext(
                type,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"),
                        new BigDecimal("550.00"),
                        new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        compositeIndex, Map.of()),
                dimensions,
                List.of(), List.of(), List.of(), List.of(),
                discountPercent, gstRate,
                new CalculationConstants(
                        new BigDecimal("0.50"),
                        new BigDecimal("0.25"),
                        new BigDecimal("0.10")));
    }
}
