package com.wd.api.estimation.calc.mapping;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.domain.enums.SiteFeeMode;
import com.wd.api.estimation.service.calc.mapping.*;
import com.wd.api.estimation.service.calc.view.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MappersTest {

    @Test
    void packageMapper_copiesIdAndNames() {
        UUID id = UUID.randomUUID();
        EstimationPackage e = new EstimationPackage();
        e.setId(id);
        e.setInternalName(PackageInternalName.STANDARD);
        e.setMarketingName("Signature");

        EstimationPackageView v = PackageMapper.toView(e);
        assertThat(v.id()).isEqualTo(id);
        assertThat(v.internalName()).isEqualTo(PackageInternalName.STANDARD);
        assertThat(v.marketingName()).isEqualTo("Signature");
    }

    @Test
    void rateVersionMapper_copiesThreeComponents() {
        UUID id = UUID.randomUUID();
        PackageRateVersion e = new PackageRateVersion();
        e.setId(id);
        e.setPackageId(UUID.randomUUID());
        e.setProjectType(ProjectType.NEW_BUILD);
        e.setMaterialRate(new BigDecimal("1500.00"));
        e.setLabourRate(new BigDecimal("550.00"));
        e.setOverheadRate(new BigDecimal("300.00"));
        e.setEffectiveFrom(LocalDate.of(2026, 4, 1));

        PackageRateVersionView v = RateVersionMapper.toView(e);
        assertThat(v.id()).isEqualTo(id);
        assertThat(v.materialRate()).isEqualByComparingTo("1500.00");
        assertThat(v.labourRate()).isEqualByComparingTo("550.00");
        assertThat(v.overheadRate()).isEqualByComparingTo("300.00");
    }

    @Test
    void marketIndexMapper_copiesIdDateAndComposite() {
        UUID id = UUID.randomUUID();
        MarketIndexSnapshot e = new MarketIndexSnapshot();
        e.setId(id);
        e.setSnapshotDate(LocalDate.of(2026, 4, 30));
        e.setCompositeIndex(new BigDecimal("1.0432"));
        e.setWeightsJson(Map.of("steel", "0.30"));

        MarketIndexSnapshotView v = MarketIndexMapper.toView(e);
        assertThat(v.id()).isEqualTo(id);
        assertThat(v.snapshotDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(v.compositeIndex()).isEqualByComparingTo("1.0432");
        assertThat(v.weights()).containsEntry("steel", "0.30");
    }

    @Test
    void siteFeeMapper_passesThroughLumpAndPerSqft() {
        SiteFee lump = new SiteFee();
        lump.setId(UUID.randomUUID());
        lump.setName("Soil");
        lump.setMode(SiteFeeMode.LUMP);
        lump.setLumpAmount(new BigDecimal("75000"));
        SiteFeeApplied lv = SiteFeeMapper.toView(lump);
        assertThat(lv.mode()).isEqualTo(SiteFeeMode.LUMP);
        assertThat(lv.lumpAmount()).isEqualByComparingTo("75000");
        assertThat(lv.perSqftRate()).isNull();

        SiteFee perSqft = new SiteFee();
        perSqft.setId(UUID.randomUUID());
        perSqft.setName("Excavation");
        perSqft.setMode(SiteFeeMode.PER_SQFT);
        perSqft.setPerSqftRate(new BigDecimal("12.00"));
        SiteFeeApplied pv = SiteFeeMapper.toView(perSqft);
        assertThat(pv.mode()).isEqualTo(SiteFeeMode.PER_SQFT);
        assertThat(pv.lumpAmount()).isNull();
        assertThat(pv.perSqftRate()).isEqualByComparingTo("12.00");
    }

    @Test
    void addonMapper_copiesNameAndAmount() {
        Addon e = new Addon();
        e.setId(UUID.randomUUID());
        e.setName("Solar 3kW");
        e.setLumpAmount(new BigDecimal("180000"));
        AddOnApplied v = AddonMapper.toView(e);
        assertThat(v.name()).isEqualTo("Solar 3kW");
        assertThat(v.lumpAmount()).isEqualByComparingTo("180000");
    }

    @Test
    void govtFeeMapper_copiesNameAndAmount() {
        GovtFee e = new GovtFee();
        e.setId(UUID.randomUUID());
        e.setName("Permit");
        e.setLumpAmount(new BigDecimal("25000"));
        GovtFeeApplied v = GovtFeeMapper.toView(e);
        assertThat(v.name()).isEqualTo("Permit");
        assertThat(v.lumpAmount()).isEqualByComparingTo("25000");
    }
}
