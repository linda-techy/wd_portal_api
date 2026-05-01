package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.*;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class EstimationPreviewServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private EstimationPreviewService service;

    @Test
    void smokeTest_handCalculated_grandTotal_2911650() {
        Setup s = seed();
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD,
                s.packageId,
                null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        CalculatePreviewResponse resp = service.preview(req);
        assertThat(resp.chargeableArea()).isEqualByComparingTo("1050");
        assertThat(resp.baseCost()).isEqualByComparingTo("2467500.00");
        assertThat(resp.fluctuationAdjustment()).isEqualByComparingTo("0.00");
        assertThat(resp.gst()).isEqualByComparingTo("444150.00");
        assertThat(resp.grandTotal()).isEqualByComparingTo("2911650.00");
        // BASE + GST line items emitted, no others
        assertThat(resp.lineItems()).hasSize(2);
    }

    @Test
    void unknownPackageId_throwsIllegalArgument() {
        seed();
        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD,
                UUID.randomUUID(),  // doesn't exist
                null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        assertThatThrownBy(() -> service.preview(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("package");
    }

    @Test
    void noActiveRateVersion_throwsIllegalState() {
        Setup s = seed();
        // Delete the rate version → no active row
        em.createQuery("DELETE FROM PackageRateVersion").executeUpdate();
        em.flush();

        CalculatePreviewRequest req = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD,
                s.packageId,
                null, null,
                new DimensionsDto(
                        List.of(new FloorDto("GF", new BigDecimal("35"), new BigDecimal("30"))),
                        BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));

        assertThatThrownBy(() -> service.preview(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rate version");
    }

    private Setup seed() {
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
        mi.setSnapshotDate(LocalDate.now());
        mi.setSteelRate(new BigDecimal("62.50"));
        mi.setCementRate(new BigDecimal("410.00"));
        mi.setSandRate(new BigDecimal("5800.00"));
        mi.setAggregateRate(new BigDecimal("1850.00"));
        mi.setTilesRate(new BigDecimal("38.00"));
        mi.setElectricalRate(new BigDecimal("92.00"));
        mi.setPaintsRate(new BigDecimal("285.00"));
        mi.setWeightsJson(Map.of("steel", "0.30"));
        mi.setCompositeIndex(new BigDecimal("1.0000"));
        mi.setActive(true);
        em.persist(mi);
        em.flush();

        return new Setup(pkg.getId());
    }

    private record Setup(UUID packageId) {}
}
