package com.wd.api.service;

import com.wd.api.model.WebhookEventLog;
import com.wd.api.model.enums.ReminderKind;
import com.wd.api.repository.WebhookEventLogRepository;
import com.wd.api.scheduler.PaymentMilestoneReminderJob.ReminderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookPublisherServicePaymentMilestoneTest {

    @Mock WebhookEventLogRepository repo;

    private WebhookPublisherService publisher;

    @BeforeEach
    void setUp() {
        publisher = new WebhookPublisherService(repo);
        // Configure a dummy webhook URL so publish() doesn't short-circuit.
        ReflectionTestUtils.setField(publisher, "webhookUrl", "http://localhost:0/webhooks/portal");
        ReflectionTestUtils.setField(publisher, "webhookSecret", "secret");
        // Make repo.save() return whatever was passed in.
        when(repo.save(any(WebhookEventLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void publishPaymentMilestoneDue_persistsLogWithExpectedShape() {
        ReminderContext ctx = new ReminderContext(
                42L, 113L, 4, "Plastering",
                LocalDate.of(2026, 5, 13),
                new BigDecimal("425000.00"));

        publisher.publishPaymentMilestoneDue(ReminderKind.T_MINUS_3, ctx);

        // publish() saves once on PENDING, then attemptDelivery() saves again
        // after the (failing) HTTP attempt — capture all and use the first.
        ArgumentCaptor<WebhookEventLog> capt = ArgumentCaptor.forClass(WebhookEventLog.class);
        org.mockito.Mockito.verify(repo, atLeastOnce()).save(capt.capture());
        WebhookEventLog log = capt.getAllValues().get(0);

        assertThat(log.getEventType()).isEqualTo("PAYMENT_MILESTONE_DUE");
        assertThat(log.getProjectId()).isEqualTo(42L);
        // referenceId = stage_id (matches existing convention: see publishInvoiceIssued).
        assertThat(log.getReferenceId()).isEqualTo(113L);
        // Payload (JSON) contains all the fields the customer-API will need.
        assertThat(log.getPayload())
                .contains("\"reminderKind\":\"T_MINUS_3\"")
                .contains("\"stageNumber\":\"4\"")
                .contains("\"stageName\":\"Plastering\"")
                .contains("\"dueDate\":\"2026-05-13\"")
                .contains("\"netPayableAmount\":\"425000.00\"");
    }

    @Test
    void publishPaymentMilestoneDue_threeKinds_eachProducesSeparateLog() {
        ReminderContext ctx = new ReminderContext(
                42L, 113L, 4, "Plastering",
                LocalDate.of(2026, 5, 13),
                new BigDecimal("425000.00"));

        publisher.publishPaymentMilestoneDue(ReminderKind.T_MINUS_3, ctx);
        publisher.publishPaymentMilestoneDue(ReminderKind.DUE_TODAY, ctx);
        publisher.publishPaymentMilestoneDue(ReminderKind.OVERDUE, ctx);

        // Three publishes × (initial PENDING save + post-delivery save) = 6 saves
        org.mockito.Mockito.verify(repo, atLeastOnce()).save(any(WebhookEventLog.class));
        // More precise: there must be at least 3 distinct PENDING saves (one per call).
        ArgumentCaptor<WebhookEventLog> capt = ArgumentCaptor.forClass(WebhookEventLog.class);
        org.mockito.Mockito.verify(repo, org.mockito.Mockito.atLeast(3)).save(capt.capture());
    }
}
