package com.wd.api.estimation.calc.view;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.PricingMode;
import com.wd.api.estimation.domain.enums.SiteFeeMode;
import com.wd.api.estimation.service.calc.view.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EstimationViewRecordsTest {

    @Test
    void floor_holdsLengthAndWidth() {
        Floor f = new Floor("GF", new BigDecimal("35"), new BigDecimal("30"));
        assertThat(f.floorName()).isEqualTo("GF");
        assertThat(f.length()).isEqualByComparingTo("35");
        assertThat(f.width()).isEqualByComparingTo("30");
    }

    @Test
    void dimensions_holdsFloorsAndAreas() {
        Dimensions d = new Dimensions(
                List.of(new Floor("GF", new BigDecimal("35"), new BigDecimal("30"))),
                new BigDecimal("200"),
                new BigDecimal("350"));
        assertThat(d.floors()).hasSize(1);
        assertThat(d.semiCoveredArea()).isEqualByComparingTo("200");
        assertThat(d.openTerraceArea()).isEqualByComparingTo("350");
    }

    @Test
    void estimationPackageView_holdsIdAndNames() {
        UUID id = UUID.randomUUID();
        EstimationPackageView v = new EstimationPackageView(id, PackageInternalName.STANDARD, "Signature");
        assertThat(v.id()).isEqualTo(id);
        assertThat(v.internalName()).isEqualTo(PackageInternalName.STANDARD);
        assertThat(v.marketingName()).isEqualTo("Signature");
    }

    @Test
    void packageRateVersionView_holdsThreeComponents() {
        UUID id = UUID.randomUUID();
        PackageRateVersionView v = new PackageRateVersionView(
                id,
                new BigDecimal("1500.00"),
                new BigDecimal("550.00"),
                new BigDecimal("300.00"));
        assertThat(v.materialRate()).isEqualByComparingTo("1500.00");
        assertThat(v.labourRate()).isEqualByComparingTo("550.00");
        assertThat(v.overheadRate()).isEqualByComparingTo("300.00");
    }

    @Test
    void marketIndexSnapshotView_holdsCompositeIndex() {
        UUID id = UUID.randomUUID();
        MarketIndexSnapshotView v = new MarketIndexSnapshotView(
                id, LocalDate.of(2026, 4, 30), new BigDecimal("1.0432"), Map.of("steel", "0.30"));
        assertThat(v.compositeIndex()).isEqualByComparingTo("1.0432");
        assertThat(v.snapshotDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void customisationChoice_holdsDeltaAndArea() {
        CustomisationChoice c = new CustomisationChoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Italian Marble", PricingMode.PER_SQFT,
                new BigDecimal("770.00"), new BigDecimal("1050.00"));
        assertThat(c.deltaRate()).isEqualByComparingTo("770.00");
        assertThat(c.applicableArea()).isEqualByComparingTo("1050.00");
    }

    @Test
    void siteFeeApplied_canBeLumpOrPerSqft() {
        SiteFeeApplied lump = new SiteFeeApplied(
                UUID.randomUUID(), "Soil surcharge", SiteFeeMode.LUMP,
                new BigDecimal("75000.00"), null);
        assertThat(lump.mode()).isEqualTo(SiteFeeMode.LUMP);
        assertThat(lump.lumpAmount()).isEqualByComparingTo("75000.00");
        assertThat(lump.perSqftRate()).isNull();

        SiteFeeApplied perSqft = new SiteFeeApplied(
                UUID.randomUUID(), "Excavation", SiteFeeMode.PER_SQFT,
                null, new BigDecimal("12.00"));
        assertThat(perSqft.lumpAmount()).isNull();
        assertThat(perSqft.perSqftRate()).isEqualByComparingTo("12.00");
    }

    @Test
    void addOnApplied_holdsLumpAmount() {
        AddOnApplied a = new AddOnApplied(UUID.randomUUID(), "Solar 3kW", new BigDecimal("180000.00"));
        assertThat(a.name()).isEqualTo("Solar 3kW");
        assertThat(a.lumpAmount()).isEqualByComparingTo("180000.00");
    }

    @Test
    void govtFeeApplied_holdsLumpAmount() {
        GovtFeeApplied f = new GovtFeeApplied(UUID.randomUUID(), "Building permit", new BigDecimal("25000.00"));
        assertThat(f.name()).isEqualTo("Building permit");
        assertThat(f.lumpAmount()).isEqualByComparingTo("25000.00");
    }

    @Test
    void calculationConstants_holdsFactorsAndRates() {
        CalculationConstants c = new CalculationConstants(
                new BigDecimal("0.50"), new BigDecimal("0.25"), new BigDecimal("0.10"));
        assertThat(c.semiCoveredFactor()).isEqualByComparingTo("0.50");
        assertThat(c.openTerraceFactor()).isEqualByComparingTo("0.25");
        assertThat(c.customisationDriftThreshold()).isEqualByComparingTo("0.10");
    }
}
