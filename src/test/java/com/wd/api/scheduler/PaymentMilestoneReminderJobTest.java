package com.wd.api.scheduler;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.model.enums.ReminderKind;
import com.wd.api.repository.PaymentStageRepository;
import com.wd.api.repository.PaymentStageReminderSentRepository;
import com.wd.api.service.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentMilestoneReminderJobTest {

    @Mock PaymentStageRepository stageRepo;
    @Mock PaymentStageReminderSentRepository reminderRepo;
    @Mock WebhookPublisherService webhookPublisher;

    /** Pinned to 2026-05-10 IST 09:00. The cron in production fires at this exact instant. */
    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-05-10T03:30:00Z"), ZoneId.of("Asia/Kolkata"));

    @InjectMocks
    private PaymentMilestoneReminderJob job;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        // Re-create job with the fixed clock — @InjectMocks doesn't pass the clock.
        job = new PaymentMilestoneReminderJob(stageRepo, reminderRepo, webhookPublisher, fixedClock);

        project = new CustomerProject();
        project.setId(42L);
        // CustomerProject.customer is not exercised here — webhook payload uses project.id only.
    }

    private PaymentStage stage(long id, int num, String name, LocalDate due, PaymentStageStatus status) {
        PaymentStage s = new PaymentStage();
        s.setId(id);
        s.setProject(project);
        s.setStageNumber(num);
        s.setStageName(name);
        s.setDueDate(due);
        s.setStatus(status);
        s.setNetPayableAmount(new BigDecimal("425000.00"));
        return s;
    }

    @Test
    void run_classifiesByDueDateRelativeToToday() {
        // today = 2026-05-10
        PaymentStage tMinus3   = stage(101L, 1, "Plastering", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE);
        PaymentStage dueToday  = stage(102L, 2, "Painting",   LocalDate.of(2026, 5, 10), PaymentStageStatus.DUE);
        PaymentStage overdue   = stage(103L, 3, "Wiring",     LocalDate.of(2026, 5, 8),  PaymentStageStatus.OVERDUE);

        when(stageRepo.findCandidatesForReminder())
                .thenReturn(List.of(tMinus3, dueToday, overdue));
        when(reminderRepo.insertIfAbsent(any(), any())).thenReturn(true);

        job.run();

        verify(reminderRepo).insertIfAbsent(101L, ReminderKind.T_MINUS_3);
        verify(reminderRepo).insertIfAbsent(102L, ReminderKind.DUE_TODAY);
        verify(reminderRepo).insertIfAbsent(103L, ReminderKind.OVERDUE);
        verify(webhookPublisher, times(3)).publishPaymentMilestoneDue(any(), any());
    }

    @Test
    void run_skipsPaidAndOnHoldAndInvoicedPastDueAndOddOffsets() {
        // The repository would never return PAID / ON_HOLD (filtered in SQL),
        // but if a future schema change lets one through, the job must still skip it.
        PaymentStage paidT3       = stage(201L, 1, "PAID @ T+3",       LocalDate.of(2026, 5, 13), PaymentStageStatus.PAID);
        PaymentStage onHoldT3     = stage(202L, 2, "ON_HOLD @ T+3",    LocalDate.of(2026, 5, 13), PaymentStageStatus.ON_HOLD);
        PaymentStage invoicedPast = stage(203L, 3, "INVOICED past",    LocalDate.of(2026, 5, 8),  PaymentStageStatus.INVOICED);
        PaymentStage oddOffset    = stage(204L, 4, "DUE @ T+5",        LocalDate.of(2026, 5, 15), PaymentStageStatus.DUE);

        when(stageRepo.findCandidatesForReminder())
                .thenReturn(List.of(paidT3, onHoldT3, invoicedPast, oddOffset));

        job.run();

        verifyNoInteractions(webhookPublisher);
        verify(reminderRepo, never()).insertIfAbsent(any(), any());
    }

    @Test
    void run_publishesPayloadWithExpectedShape() {
        PaymentStage stage = stage(113L, 4, "Plastering", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE);
        when(stageRepo.findCandidatesForReminder()).thenReturn(List.of(stage));
        when(reminderRepo.insertIfAbsent(113L, ReminderKind.T_MINUS_3)).thenReturn(true);

        job.run();

        ArgumentCaptor<PaymentMilestoneReminderJob.ReminderContext> ctx =
                ArgumentCaptor.forClass(PaymentMilestoneReminderJob.ReminderContext.class);
        verify(webhookPublisher).publishPaymentMilestoneDue(eq(ReminderKind.T_MINUS_3), ctx.capture());

        PaymentMilestoneReminderJob.ReminderContext c = ctx.getValue();
        assertThat(c.projectId()).isEqualTo(42L);
        assertThat(c.stageId()).isEqualTo(113L);
        assertThat(c.stageNumber()).isEqualTo(4);
        assertThat(c.stageName()).isEqualTo("Plastering");
        assertThat(c.dueDate()).isEqualTo(LocalDate.of(2026, 5, 13));
        assertThat(c.netPayableAmount()).isEqualByComparingTo("425000.00");
    }

    @Test
    void run_idempotent_secondRunSendsNothingExtra() {
        PaymentStage stage = stage(101L, 1, "Plastering", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE);
        when(stageRepo.findCandidatesForReminder()).thenReturn(List.of(stage));

        // First run: insert succeeds → publish.
        when(reminderRepo.insertIfAbsent(101L, ReminderKind.T_MINUS_3)).thenReturn(true);
        job.run();
        verify(webhookPublisher, times(1)).publishPaymentMilestoneDue(any(), any());

        // Second run: same fixedClock, same candidate → insert returns false → NO publish.
        when(reminderRepo.insertIfAbsent(101L, ReminderKind.T_MINUS_3)).thenReturn(false);
        job.run();
        verify(webhookPublisher, times(1)).publishPaymentMilestoneDue(any(), any()); // unchanged
    }

    @Test
    void run_insertHappensBeforePublish() {
        // Spec: insert FIRST, publish SECOND. If publish fails after insert commits,
        // the customer doesn't get that day's reminder — accepted (under-notify > over-notify).
        PaymentStage stage = stage(101L, 1, "Plastering", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE);
        when(stageRepo.findCandidatesForReminder()).thenReturn(List.of(stage));
        when(reminderRepo.insertIfAbsent(101L, ReminderKind.T_MINUS_3)).thenReturn(true);

        job.run();

        InOrderAssertion.assertInOrder(reminderRepo, webhookPublisher);
    }

    /** Tiny helper so the order assertion reads cleanly above. */
    static class InOrderAssertion {
        static void assertInOrder(PaymentStageReminderSentRepository repo, WebhookPublisherService pub) {
            org.mockito.InOrder order = inOrder(repo, pub);
            order.verify(repo).insertIfAbsent(any(), any());
            order.verify(pub).publishPaymentMilestoneDue(any(), any());
        }
    }

    @Test
    void run_handlesEmptyCandidateList() {
        when(stageRepo.findCandidatesForReminder()).thenReturn(new ArrayList<>());

        job.run();

        verifyNoInteractions(reminderRepo);
        verifyNoInteractions(webhookPublisher);
    }
}
