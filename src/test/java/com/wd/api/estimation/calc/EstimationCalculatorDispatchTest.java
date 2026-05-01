package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.EstimationCalculator;
import com.wd.api.estimation.service.calc.EstimationContext;
import com.wd.api.estimation.service.calc.exception.UnsupportedProjectTypeException;
import com.wd.api.estimation.service.calc.view.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstimationCalculatorDispatchTest {

    private final EstimationCalculator calculator = new EstimationCalculator();

    @Test
    void newBuild_dispatchesAndReturnsBreakdown() {
        EstimationContext ctx = newBuildContext();
        assertThat(calculator.calculate(ctx)).isNotNull();
    }

    @Test
    void commercial_dispatchesAndReturnsBreakdown() {
        EstimationContext ctx = commercialContext();
        assertThat(calculator.calculate(ctx)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = ProjectType.class, names = {"RENOVATION", "INTERIOR", "COMPOUND"})
    void otherProjectTypes_throwUnsupported(ProjectType type) {
        EstimationContext ctx = contextOf(type);
        assertThatThrownBy(() -> calculator.calculate(ctx))
                .isInstanceOf(UnsupportedProjectTypeException.class)
                .hasMessageContaining(type.name());
    }

    private EstimationContext newBuildContext() { return contextOf(ProjectType.NEW_BUILD); }
    private EstimationContext commercialContext() { return contextOf(ProjectType.COMMERCIAL); }

    static EstimationContext contextOf(ProjectType type) {
        return new EstimationContext(
                type,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                new Dimensions(List.of(new Floor("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));
    }
}
