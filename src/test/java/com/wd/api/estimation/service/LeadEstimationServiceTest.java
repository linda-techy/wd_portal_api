package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.*;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LeadEstimationServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private LeadEstimationService service;

    private static final Long LEAD_ID = 1L;

    private UUID packageId;

    @BeforeEach
    void seed() {
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

        packageId = pkg.getId();
    }

    private LeadEstimationCreateRequest standardRequest() {
        DimensionsDto dim = new DimensionsDto(
                List.of(new FloorDto("Ground", new BigDecimal("30"), new BigDecimal("35"))),
                BigDecimal.ZERO, BigDecimal.ZERO);
        CalculatePreviewRequest preview = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId,
                null, null, dim, List.of(), List.of(), List.of(), List.of(),
                null, null);
        return new LeadEstimationCreateRequest(LEAD_ID, preview, null);
    }

    @Test
    void create_persistsEstimationWithLineItems() {
        LeadEstimationDetailResponse resp = service.create(standardRequest());
        assertThat(resp.estimationNo()).startsWith("EST-");
        assertThat(resp.grandTotal()).isPositive();
        assertThat(resp.lineItems()).isNotEmpty();
        assertThat(resp.status()).isEqualTo(EstimationStatus.DRAFT);
        assertThat(resp.rateVersionId()).isNotNull();
        assertThat(resp.marketIndexId()).isNotNull();
    }

    @Test
    void listByLead_returnsNewestFirst() {
        service.create(standardRequest());
        service.create(standardRequest());

        List<LeadEstimationSummaryResponse> items = service.listByLead(LEAD_ID);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).createdAt()).isAfterOrEqualTo(items.get(1).createdAt());
    }

    @Test
    void delete_softDeletesEstimation() {
        LeadEstimationDetailResponse created = service.create(standardRequest());
        service.delete(created.id());

        List<LeadEstimationSummaryResponse> remaining = service.listByLead(LEAD_ID);
        assertThat(remaining).isEmpty();
    }
}
