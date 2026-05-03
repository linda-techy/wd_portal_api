package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.PackageInternalName;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.dto.*;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@Transactional
class EstimationPdfServiceTest extends TestcontainersPostgresBase {

    @Autowired private EstimationPdfService pdfService;
    @Autowired private LeadEstimationService leadEstimationService;
    @Autowired private EstimationSubResourceService subResourceService;
    @Autowired private EntityManager em;

    private UUID packageId;

    @BeforeEach
    void seed() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("Signature");
        pkg.setDisplayOrder(20);
        em.persist(pkg);
        packageId = pkg.getId();

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
    }

    @Test
    void generatePdf_returnsNonEmptyPdfBytes() {
        // Create a minimal estimation via service
        DimensionsDto dim = new DimensionsDto(
                List.of(new FloorDto("Ground", new BigDecimal("30"), new BigDecimal("35"))),
                BigDecimal.ZERO, BigDecimal.ZERO);
        CalculatePreviewRequest preview = new CalculatePreviewRequest(
                ProjectType.NEW_BUILD, packageId, null, null, dim,
                List.of(), List.of(), List.of(), List.of(),
                BigDecimal.ZERO, new BigDecimal("0.18"));
        LeadEstimationCreateRequest req = new LeadEstimationCreateRequest(1L, preview, null);

        LeadEstimationDetailResponse created = leadEstimationService.create(req);
        UUID estimationId = created.id();

        em.flush();
        em.clear();

        // Add 1 inclusion
        subResourceService.create(estimationId, SubResourceType.INCLUSION,
                new EstimationSubResourceRequest("All materials included", "As per spec", 1, null));

        // Add 1 milestone (100% so sum is valid)
        subResourceService.create(estimationId, SubResourceType.PAYMENT_MILESTONE,
                new EstimationSubResourceRequest("On completion", null, 1, new BigDecimal("100.00")));

        em.flush();
        em.clear();

        // Generate PDF and assert it starts with the %PDF- magic bytes
        byte[] pdf = pdfService.generatePdf(estimationId);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
