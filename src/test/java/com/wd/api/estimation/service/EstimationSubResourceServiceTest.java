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

import static org.assertj.core.api.Assertions.*;

@Transactional
class EstimationSubResourceServiceTest extends TestcontainersPostgresBase {

    @Autowired private EntityManager em;
    @Autowired private EstimationSubResourceService service;

    private UUID estimationId;

    @BeforeEach
    void seed() {
        EstimationPackage pkg = new EstimationPackage();
        pkg.setInternalName(PackageInternalName.STANDARD);
        pkg.setMarketingName("TestPkg");
        em.persist(pkg);

        Estimation est = new Estimation();
        est.setEstimationNo("EST-TEST-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        est.setLeadId(99L);
        est.setProjectType(ProjectType.NEW_BUILD);
        est.setPackageId(pkg.getId());
        est.setDimensionsJson(Map.of("floors", List.of()));
        est.setStatus(EstimationStatus.DRAFT);
        est.setSubtotal(BigDecimal.ZERO);
        est.setDiscountAmount(BigDecimal.ZERO);
        est.setGstAmount(BigDecimal.ZERO);
        est.setGrandTotal(BigDecimal.ZERO);
        est.setValidUntil(LocalDate.now().plusDays(30));
        em.persist(est);
        em.flush();

        estimationId = est.getId();
    }

    // -------------------------------------------------------------------------
    // 1. create + list returns the new item ordered by displayOrder (INCLUSION)
    // -------------------------------------------------------------------------

    @Test
    void create_and_list_inclusion_ordered_by_displayOrder() {
        EstimationSubResourceRequest req2 = new EstimationSubResourceRequest("B item", null, 2, null);
        EstimationSubResourceRequest req1 = new EstimationSubResourceRequest("A item", null, 1, null);

        service.create(estimationId, SubResourceType.INCLUSION, req2);
        service.create(estimationId, SubResourceType.INCLUSION, req1);

        List<EstimationSubResourceResponse> result = service.list(estimationId, SubResourceType.INCLUSION);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).label()).isEqualTo("A item");
        assertThat(result.get(0).displayOrder()).isEqualTo(1);
        assertThat(result.get(1).label()).isEqualTo("B item");
        assertThat(result.get(1).displayOrder()).isEqualTo(2);
        assertThat(result.get(0).percentage()).isNull();
    }

    // -------------------------------------------------------------------------
    // 2. create payment milestone with valid percentage succeeds
    // -------------------------------------------------------------------------

    @Test
    void create_paymentMilestone_with_valid_percentage_succeeds() {
        EstimationSubResourceRequest req = new EstimationSubResourceRequest(
                "Foundation", "Paid at slab level", 1, new BigDecimal("50.00"));

        EstimationSubResourceResponse resp = service.create(estimationId, SubResourceType.PAYMENT_MILESTONE, req);

        assertThat(resp.id()).isNotNull();
        assertThat(resp.estimationId()).isEqualTo(estimationId);
        assertThat(resp.label()).isEqualTo("Foundation");
        assertThat(resp.percentage()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // -------------------------------------------------------------------------
    // 3. create payment milestone without percentage throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void create_paymentMilestone_without_percentage_throws() {
        EstimationSubResourceRequest req = new EstimationSubResourceRequest(
                "Foundation", null, 1, null);

        assertThatThrownBy(() -> service.create(estimationId, SubResourceType.PAYMENT_MILESTONE, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentage is required for PAYMENT_MILESTONE");
    }

    // -------------------------------------------------------------------------
    // 4. create non-milestone WITH percentage throws IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void create_nonMilestone_with_percentage_throws() {
        EstimationSubResourceRequest req = new EstimationSubResourceRequest(
                "Structural work included", null, 1, new BigDecimal("50.00"));

        assertThatThrownBy(() -> service.create(estimationId, SubResourceType.INCLUSION, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentage must not be provided for type INCLUSION");
    }

    // -------------------------------------------------------------------------
    // 5. delete soft-deletes — subsequent list returns empty
    // -------------------------------------------------------------------------

    @Test
    void delete_softDeletes_and_listReturnsEmpty() {
        EstimationSubResourceRequest req = new EstimationSubResourceRequest("To delete", null, 0, null);
        EstimationSubResourceResponse created = service.create(estimationId, SubResourceType.EXCLUSION, req);

        assertThat(service.list(estimationId, SubResourceType.EXCLUSION)).hasSize(1);

        service.delete(estimationId, SubResourceType.EXCLUSION, created.id());

        assertThat(service.list(estimationId, SubResourceType.EXCLUSION)).isEmpty();
    }
}
