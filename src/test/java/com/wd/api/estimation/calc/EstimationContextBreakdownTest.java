package com.wd.api.estimation.calc;

import com.wd.api.estimation.domain.enums.LineType;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.EstimationBreakdown;
import com.wd.api.estimation.service.calc.EstimationContext;
import com.wd.api.estimation.service.calc.LineItem;
import com.wd.api.estimation.service.calc.view.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EstimationContextBreakdownTest {

    @Test
    void context_canBeConstructed() {
        EstimationContext ctx = new EstimationContext(
                ProjectType.NEW_BUILD,
                new EstimationPackageView(UUID.randomUUID(), PackageInternalName.STANDARD, "Signature"),
                new PackageRateVersionView(UUID.randomUUID(),
                        new BigDecimal("1500.00"), new BigDecimal("550.00"), new BigDecimal("300.00")),
                new MarketIndexSnapshotView(UUID.randomUUID(), LocalDate.of(2026, 4, 30),
                        new BigDecimal("1.0000"), Map.of()),
                new Dimensions(List.of(new Floor("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                new BigDecimal("0.18"),
                new CalculationConstants(new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10")));

        assertThat(ctx.projectType()).isEqualTo(ProjectType.NEW_BUILD);
        assertThat(ctx.gstRate()).isEqualByComparingTo("0.18");
    }

    @Test
    void breakdown_canBeConstructed() {
        EstimationBreakdown b = new EstimationBreakdown(
                new BigDecimal("1050.00"),  // chargeableArea
                new BigDecimal("2467500.00"),  // baseCost
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO,  // fluctuationAdjustment
                new BigDecimal("2467500.00"),  // subtotal
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("2467500.00"),  // taxable
                new BigDecimal("444150.00"),  // gst
                new BigDecimal("2911650.00"),  // grandTotal
                List.of(),
                List.of());
        assertThat(b.grandTotal()).isEqualByComparingTo("2911650.00");
        assertThat(b.warnings()).isEmpty();
    }

    @Test
    void lineItem_holdsAllFields() {
        UUID source = UUID.randomUUID();
        LineItem li = new LineItem(
                LineType.BASE,
                "Base package cost (Standard, 1050 sqft)",
                source,
                new BigDecimal("1050.00"),
                "sqft",
                new BigDecimal("2350.00"),
                new BigDecimal("2467500.00"),
                1);
        assertThat(li.lineType()).isEqualTo(LineType.BASE);
        assertThat(li.amount()).isEqualByComparingTo("2467500.00");
        assertThat(li.displayOrder()).isEqualTo(1);
    }
}
