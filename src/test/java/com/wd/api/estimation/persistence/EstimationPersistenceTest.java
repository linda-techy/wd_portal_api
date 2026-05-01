package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.model.Lead;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EstimationPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_estimation() {
        Lead lead = new Lead();
        lead.setName("Test Customer");
        lead.setEmail("test@example.com");
        em.persist(lead);

        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        em.persist(pkg);

        PackageRateVersion rv = new PackageRateVersion();
        rv.setPackageId(pkg.getId());
        rv.setProjectType(ProjectType.NEW_BUILD);
        rv.setMaterialRate(new BigDecimal("1500.00"));
        rv.setLabourRate(new BigDecimal("550.00"));
        rv.setOverheadRate(new BigDecimal("300.00"));
        rv.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        em.persist(rv);

        MarketIndexSnapshot mi = new MarketIndexSnapshot();
        mi.setSnapshotDate(LocalDate.of(2026, 4, 30));
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.<String, Object>of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        mi.setActive(true);
        em.persist(mi);
        em.flush();

        Estimation est = new Estimation();
        est.setEstimationNo("EST-2026-0001");
        est.setLeadId(lead.getId());
        est.setProjectType(ProjectType.NEW_BUILD);
        est.setPackageId(pkg.getId());
        est.setRateVersionId(rv.getId());
        est.setMarketIndexId(mi.getId());
        est.setDimensionsJson(Map.<String, Object>of(
                "floors", "[{\"floorName\":\"GF\",\"length\":35,\"width\":30}]",
                "semiCoveredArea", "0",
                "openTerraceArea", "0"));
        est.setStatus(EstimationStatus.DRAFT);
        est.setSubtotal(new BigDecimal("2467500.00"));
        est.setDiscountAmount(BigDecimal.ZERO);
        est.setGstAmount(new BigDecimal("444150.00"));
        est.setGrandTotal(new BigDecimal("2911650.00"));
        est.setValidUntil(LocalDate.of(2026, 6, 30));

        em.persist(est);
        em.flush();
        UUID id = est.getId();
        em.clear();

        Estimation loaded = em.find(Estimation.class, id);
        assertThat(loaded.getEstimationNo()).isEqualTo("EST-2026-0001");
        assertThat(loaded.getLeadId()).isEqualTo(lead.getId());
        assertThat(loaded.getStatus()).isEqualTo(EstimationStatus.DRAFT);
        assertThat(loaded.getGrandTotal()).isEqualByComparingTo("2911650.00");
        assertThat(loaded.getDimensionsJson()).containsKey("floors");
        assertThat(loaded.getVersion()).isEqualTo(1L);
    }
}
