package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the legacy {@code ProjectTrackingService.approveVariation} path
 * to delegate to {@link ProjectVariationService#approveByCustomer} (or
 * {@link ProjectVariationService#reject} when {@code approve == false}).
 * Prevents drift while the legacy method is still callable.
 */
class ProjectTrackingServiceConsolidationTest extends TestcontainersPostgresBase {

    @Autowired private ProjectTrackingService trackingService;
    @MockitoSpyBean private ProjectVariationService variationService;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private PortalUserRepository userRepo;

    private Long projectId;
    private Long approverId;

    @BeforeEach
    void setup() {
        CustomerProject p = new CustomerProject();
        p.setName("trk-" + UUID.randomUUID());
        p.setLocation("L"); p.setProjectUuid(UUID.randomUUID());
        projectId = projectRepo.save(p).getId();

        PortalUser u = new PortalUser();
        u.setEmail("u-" + UUID.randomUUID() + "@t");
        u.setFirstName("u"); u.setLastName("u"); u.setPassword("x"); u.setEnabled(true);
        approverId = userRepo.save(u).getId();
    }

    private ProjectVariation walkToCustomerApprovalPending() {
        ProjectVariation cr = ProjectVariation.builder()
                .description("d").estimatedAmount(new BigDecimal("1")).build();
        cr = variationService.createVariation(cr, projectId, approverId);
        cr = variationService.submit(cr.getId(), approverId);
        cr = variationService.cost(cr.getId(), new BigDecimal("1"), 1, approverId);
        cr = variationService.sendToCustomer(cr.getId(), approverId);
        return cr;
    }

    @Test
    void approveVariation_TrueDelegatesToApproveByCustomer() {
        ProjectVariation cr = walkToCustomerApprovalPending();
        Mockito.clearInvocations(variationService);

        ProjectVariation result = trackingService.approveVariation(cr.getId(), approverId, true);

        Mockito.verify(variationService).approveByCustomer(
                Mockito.eq(cr.getId()), Mockito.isNull(),
                Mockito.isNull(), Mockito.isNull());
        assertThat(result.getStatus()).isEqualTo(VariationStatus.APPROVED);
    }

    @Test
    void approveVariation_FalseDelegatesToReject() {
        ProjectVariation cr = walkToCustomerApprovalPending();
        Mockito.clearInvocations(variationService);

        ProjectVariation result = trackingService.approveVariation(cr.getId(), approverId, false);

        Mockito.verify(variationService).reject(
                Mockito.eq(cr.getId()), Mockito.anyString(), Mockito.eq(approverId));
        assertThat(result.getStatus()).isEqualTo(VariationStatus.REJECTED);
    }
}
