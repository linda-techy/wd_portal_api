package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.service.changerequest.ChangeRequestMergeService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the 8 legal transitions + rejection-from-any-state + a sample
 * of illegal transitions. Spring slice on Postgres testcontainer.
 */
class ProjectVariationServiceStateMachineTest extends TestcontainersPostgresBase {

    @Autowired private ProjectVariationService service;
    @Autowired private ProjectVariationRepository crRepo;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private PortalUserRepository userRepo;
    /**
     * S4 PR2 wires {@link ChangeRequestMergeService} into
     * {@link ProjectVariationService#schedule}. The state-machine test only
     * cares about the legal/illegal transition envelope, not the merge
     * semantics — covered by ChangeRequestMergeServiceTest. We mock the merge
     * to a no-op result so the test's bogus anchor task ids do not trip the
     * "anchor not in project" guard in production code.
     */
    @MockitoBean private ChangeRequestMergeService mergeService;

    private Long projectId;
    private Long actorUserId;
    private Long customerUserId;

    @BeforeEach
    void setup() {
        // Stub the merge so schedule()'s PR2 hook is a no-op for state-machine tests.
        when(mergeService.mergeIntoWbs(anyLong(), anyLong(), any()))
                .thenReturn(new ChangeRequestMergeResult(0, 0, 0, true));

        CustomerProject p = new CustomerProject();
        p.setName("svc-sm-" + UUID.randomUUID());
        p.setLocation("Loc");
        p.setProjectUuid(UUID.randomUUID());
        p = projectRepo.save(p);
        projectId = p.getId();

        PortalUser u = new PortalUser();
        u.setEmail("actor-" + UUID.randomUUID() + "@test.local");
        u.setFirstName("Actor");
        u.setLastName("User");
        u.setPassword("x");
        u.setEnabled(true);
        u = userRepo.save(u);
        actorUserId = u.getId();

        PortalUser c = new PortalUser();
        c.setEmail("customer-" + UUID.randomUUID() + "@test.local");
        c.setFirstName("Customer");
        c.setLastName("User");
        c.setPassword("x");
        c.setEnabled(true);
        c = userRepo.save(c);
        customerUserId = c.getId();
    }

    private ProjectVariation newDraft() {
        ProjectVariation cr = ProjectVariation.builder()
                .description("d")
                .estimatedAmount(new BigDecimal("100"))
                .build();
        return service.createVariation(cr, projectId, actorUserId);
    }

    // ---- 8 legal transitions ----

    @Test
    void submit_DraftToSubmitted() {
        ProjectVariation cr = newDraft();
        cr = service.submit(cr.getId(), actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.SUBMITTED);
        assertThat(cr.getSubmittedAt()).isNotNull();
    }

    @Test
    void cost_SubmittedToCosted() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorUserId);
        cr = service.cost(cr.getId(), new BigDecimal("125000.00"), 12, actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.COSTED);
        assertThat(cr.getCostImpact()).isEqualByComparingTo("125000.00");
        assertThat(cr.getTimeImpactWorkingDays()).isEqualTo(12);
        assertThat(cr.getCostedAt()).isNotNull();
    }

    @Test
    void sendToCustomer_CostedToCustomerApprovalPending() {
        ProjectVariation cr = newDraft();
        cr = service.submit(cr.getId(), actorUserId);
        cr = service.cost(cr.getId(), new BigDecimal("100"), 1, actorUserId);
        cr = service.sendToCustomer(cr.getId(), actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.CUSTOMER_APPROVAL_PENDING);
        assertThat(cr.getSentToCustomerAt()).isNotNull();
    }

    @Test
    void approveByCustomer_CustomerApprovalPendingToApproved() {
        ProjectVariation cr = newDraft();
        cr = service.submit(cr.getId(), actorUserId);
        cr = service.cost(cr.getId(), new BigDecimal("100"), 1, actorUserId);
        cr = service.sendToCustomer(cr.getId(), actorUserId);
        cr = service.approveByCustomer(cr.getId(), customerUserId, "deadbeef", "10.0.0.1");
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.APPROVED);
        assertThat(cr.getApprovedAt()).isNotNull();
        assertThat(cr.getClientApproved()).isTrue();
    }

    @Test
    void schedule_ApprovedToScheduled() {
        ProjectVariation cr = newDraft();
        cr = service.submit(cr.getId(), actorUserId);
        cr = service.cost(cr.getId(), new BigDecimal("100"), 1, actorUserId);
        cr = service.sendToCustomer(cr.getId(), actorUserId);
        cr = service.approveByCustomer(cr.getId(), customerUserId, "h", "ip");
        cr = service.schedule(cr.getId(), 99L, actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.SCHEDULED);
        assertThat(cr.getScheduledAt()).isNotNull();
    }

    @Test
    void start_ScheduledToInProgress() {
        ProjectVariation cr = walkUpTo(VariationStatus.SCHEDULED);
        cr = service.start(cr.getId(), actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.IN_PROGRESS);
        assertThat(cr.getStartedAt()).isNotNull();
    }

    @Test
    void complete_InProgressToComplete() {
        ProjectVariation cr = walkUpTo(VariationStatus.SCHEDULED);
        cr = service.start(cr.getId(), actorUserId);
        cr = service.complete(cr.getId(), actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.COMPLETE);
        assertThat(cr.getCompletedAt()).isNotNull();
    }

    @Test
    void reject_FromDraft_GoesToRejected() {
        ProjectVariation cr = newDraft();
        cr = service.reject(cr.getId(), "no thanks", actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.REJECTED);
        assertThat(cr.getRejectionReason()).isEqualTo("no thanks");
        assertThat(cr.getRejectedAt()).isNotNull();
    }

    // ---- REJECTED from each non-terminal state (sample of 4) ----

    @Test
    void reject_FromSubmitted_AllowedWithReason() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorUserId);
        cr = service.reject(cr.getId(), "scope creep", actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.REJECTED);
    }

    @Test
    void reject_FromCosted_Allowed() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorUserId);
        cr = service.cost(cr.getId(), new BigDecimal("1"), 1, actorUserId);
        cr = service.reject(cr.getId(), "too expensive", actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.REJECTED);
    }

    @Test
    void reject_FromCustomerApprovalPending_Allowed() {
        ProjectVariation cr = walkUpTo(VariationStatus.CUSTOMER_APPROVAL_PENDING);
        cr = service.reject(cr.getId(), "withdrawn", actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.REJECTED);
    }

    @Test
    void reject_FromInProgress_Allowed() {
        ProjectVariation cr = walkUpTo(VariationStatus.SCHEDULED);
        cr = service.start(cr.getId(), actorUserId);
        cr = service.reject(cr.getId(), "site issue", actorUserId);
        assertThat(cr.getStatus()).isEqualTo(VariationStatus.REJECTED);
    }

    @Test
    void reject_FromComplete_RejectedTerminalGuardThrows() {
        ProjectVariation cr = walkUpTo(VariationStatus.SCHEDULED);
        cr = service.start(cr.getId(), actorUserId);
        cr = service.complete(cr.getId(), actorUserId);
        Long id = cr.getId();
        assertThatThrownBy(() -> service.reject(id, "too late", actorUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reject_FromRejected_AlreadyTerminalThrows() {
        ProjectVariation cr = service.reject(newDraft().getId(), "x", actorUserId);
        Long id = cr.getId();
        assertThatThrownBy(() -> service.reject(id, "y", actorUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- Illegal transitions (sample) ----

    @Test
    void illegal_DraftToApprovedThrows() {
        ProjectVariation cr = newDraft();
        Long id = cr.getId();
        assertThatThrownBy(() -> service.approveByCustomer(id, customerUserId, "h", "ip"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegal_SubmittedToScheduledThrows() {
        ProjectVariation cr = service.submit(newDraft().getId(), actorUserId);
        Long id = cr.getId();
        assertThatThrownBy(() -> service.schedule(id, 1L, actorUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegal_ApprovedToCompleteSkipsScheduledThrows() {
        ProjectVariation cr = walkUpTo(VariationStatus.APPROVED);
        Long id = cr.getId();
        assertThatThrownBy(() -> service.complete(id, actorUserId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void illegal_RejectMissingReasonThrows() {
        ProjectVariation cr = newDraft();
        Long id = cr.getId();
        assertThatThrownBy(() -> service.reject(id, "  ", actorUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- helper ----
    private ProjectVariation walkUpTo(VariationStatus target) {
        ProjectVariation cr = newDraft();
        if (target == VariationStatus.DRAFT) return cr;
        cr = service.submit(cr.getId(), actorUserId);
        if (target == VariationStatus.SUBMITTED) return cr;
        cr = service.cost(cr.getId(), new BigDecimal("100"), 1, actorUserId);
        if (target == VariationStatus.COSTED) return cr;
        cr = service.sendToCustomer(cr.getId(), actorUserId);
        if (target == VariationStatus.CUSTOMER_APPROVAL_PENDING) return cr;
        cr = service.approveByCustomer(cr.getId(), customerUserId, "h", "ip");
        if (target == VariationStatus.APPROVED) return cr;
        cr = service.schedule(cr.getId(), 1L, actorUserId);
        if (target == VariationStatus.SCHEDULED) return cr;
        throw new IllegalArgumentException("walkUpTo not implemented for " + target);
    }
}
