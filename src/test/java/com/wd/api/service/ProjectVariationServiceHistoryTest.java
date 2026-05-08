package com.wd.api.service;

import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.ChangeRequestApprovalHistory;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.ChangeRequestApprovalHistoryRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.changerequest.ChangeRequestMergeService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class ProjectVariationServiceHistoryTest extends TestcontainersPostgresBase {

    @Autowired private ProjectVariationService service;
    @Autowired private ChangeRequestApprovalHistoryRepository historyRepo;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private PortalUserRepository userRepo;
    @MockitoBean private ChangeRequestMergeService mergeService;

    private Long projectId;
    private Long actorId;
    private Long customerId;

    @BeforeEach
    void setup() {
        when(mergeService.mergeIntoWbs(anyLong(), anyLong(), any()))
                .thenReturn(new ChangeRequestMergeResult(0, 0, 0, true));

        CustomerProject p = new CustomerProject();
        p.setName("hist-" + UUID.randomUUID());
        p.setLocation("L");
        p.setProjectUuid(UUID.randomUUID());
        projectId = projectRepo.save(p).getId();

        PortalUser u = new PortalUser();
        u.setEmail("a-" + UUID.randomUUID() + "@t");
        u.setFirstName("a"); u.setLastName("u"); u.setPassword("x"); u.setEnabled(true);
        actorId = userRepo.save(u).getId();

        PortalUser c = new PortalUser();
        c.setEmail("c-" + UUID.randomUUID() + "@t");
        c.setFirstName("c"); c.setLastName("u"); c.setPassword("x"); c.setEnabled(true);
        customerId = userRepo.save(c).getId();
    }

    private ProjectVariation newDraft() {
        ProjectVariation v = ProjectVariation.builder()
                .description("d").estimatedAmount(new BigDecimal("1")).build();
        return service.createVariation(v, projectId, actorId);
    }

    private List<ChangeRequestApprovalHistory> historyOf(ProjectVariation cr) {
        return historyRepo.findByChangeRequestIdOrderByActionAtDesc(cr.getId());
    }

    @Test
    void submit_AppendsOneRowDraftToSubmittedWithActor() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorId);
        List<ChangeRequestApprovalHistory> h = historyOf(cr);
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getFromStatus()).isEqualTo(VariationStatus.DRAFT);
        assertThat(h.get(0).getToStatus()).isEqualTo(VariationStatus.SUBMITTED);
        assertThat(h.get(0).getActorUserId()).isEqualTo(actorId);
        assertThat(h.get(0).getCustomerUserId()).isNull();
        assertThat(h.get(0).getReason()).isNull();
    }

    @Test
    void cost_AppendsOneRowSubmittedToCosted() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorId);
        cr = service.cost(cr.getId(), new BigDecimal("10"), 1, actorId);
        List<ChangeRequestApprovalHistory> h = historyOf(cr);
        assertThat(h).hasSize(2);
        assertThat(h.get(0).getFromStatus()).isEqualTo(VariationStatus.SUBMITTED);
        assertThat(h.get(0).getToStatus()).isEqualTo(VariationStatus.COSTED);
    }

    @Test
    void approveByCustomer_AppendsRowWithOtpHashAndIpAndCustomerId() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorId);
        cr = service.cost(cr.getId(), new BigDecimal("1"), 1, actorId);
        cr = service.sendToCustomer(cr.getId(), actorId);
        cr = service.approveByCustomer(cr.getId(), customerId, "abc123", "10.1.2.3");
        List<ChangeRequestApprovalHistory> h = historyOf(cr);
        assertThat(h).hasSize(4);
        ChangeRequestApprovalHistory approve = h.get(0);
        assertThat(approve.getFromStatus()).isEqualTo(VariationStatus.CUSTOMER_APPROVAL_PENDING);
        assertThat(approve.getToStatus()).isEqualTo(VariationStatus.APPROVED);
        assertThat(approve.getActorUserId()).isNull();
        assertThat(approve.getCustomerUserId()).isEqualTo(customerId);
        assertThat(approve.getOtpHash()).isEqualTo("abc123");
        assertThat(approve.getCustomerIp()).isEqualTo("10.1.2.3");
    }

    @Test
    void reject_AppendsRowWithReason() {
        ProjectVariation cr = service.reject(newDraft().getId(), "no thanks", actorId);
        List<ChangeRequestApprovalHistory> h = historyOf(cr);
        assertThat(h).hasSize(1);
        assertThat(h.get(0).getToStatus()).isEqualTo(VariationStatus.REJECTED);
        assertThat(h.get(0).getReason()).isEqualTo("no thanks");
        assertThat(h.get(0).getActorUserId()).isEqualTo(actorId);
    }

    @Test
    void schedule_StoresAnchorTaskIdInReasonField() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorId);
        cr = service.cost(cr.getId(), new BigDecimal("1"), 1, actorId);
        cr = service.sendToCustomer(cr.getId(), actorId);
        cr = service.approveByCustomer(cr.getId(), customerId, "h", "ip");
        cr = service.schedule(cr.getId(), 42L, actorId);
        ChangeRequestApprovalHistory latest = historyOf(cr).get(0);
        assertThat(latest.getToStatus()).isEqualTo(VariationStatus.SCHEDULED);
        assertThat(latest.getReason()).isEqualTo("anchorTaskId=42");
    }
}
