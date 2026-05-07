package com.wd.api.service;

import com.wd.api.model.WebhookEventLog;
import com.wd.api.repository.WebhookEventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookPublisherService.
 *
 * The service uses an internal java.net.http.HttpClient (not injectable), so tests that exercise
 * the full delivery path must supply a real (but unreachable) webhookUrl and assert on the
 * WebhookEventLog state captured via the mock repository.
 *
 * Tests that only verify persistence behaviour (PENDING save, retry logic) do not need HTTP at all
 * and can use a blank webhookUrl.
 */
@ExtendWith(MockitoExtension.class)
class WebhookPublisherServiceTest {

    @Mock
    private WebhookEventLogRepository eventLogRepository;

    @InjectMocks
    private WebhookPublisherService webhookPublisherService;

    @BeforeEach
    void setUp() {
        // Provide a real-looking URL so the service does not short-circuit silently.
        // HTTP delivery will fail (connection refused), which is what we want to test failure path.
        ReflectionTestUtils.setField(webhookPublisherService, "webhookUrl", "http://localhost:19999/webhook");
        ReflectionTestUtils.setField(webhookPublisherService, "webhookSecret", "test-secret-key-for-hmac");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private WebhookEventLog pendingLog(String eventType) {
        WebhookEventLog log = new WebhookEventLog();
        log.setEventType(eventType);
        log.setProjectId(1L);
        log.setStatus("PENDING");
        log.setAttempts(0);
        log.setPayload("{\"eventType\":\"" + eventType + "\"}");
        return log;
    }

    private WebhookPublisherService.PortalWebhookPayload testPayload(String eventType) {
        return new WebhookPublisherService.PortalWebhookPayload(
                eventType, 1L, null, 10L, "Test summary", null, LocalDateTime.now());
    }

    // ── publish: persistence before delivery ──────────────────────────────────

    @Test
    void publish_withConfiguredUrl_savesEventAsPendingBeforeDelivery() {
        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);

        WebhookEventLog pendingLog = pendingLog("SITE_REPORT_SUBMITTED");
        // First save (PENDING); second save (after delivery attempt — FAILED due to connection refused)
        when(eventLogRepository.save(captor.capture())).thenReturn(pendingLog);

        webhookPublisherService.publish(testPayload("SITE_REPORT_SUBMITTED"));

        // The very first save must record PENDING status
        WebhookEventLog firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getStatus()).isEqualTo("PENDING");
        assertThat(firstSave.getEventType()).isEqualTo("SITE_REPORT_SUBMITTED");
    }

    @Test
    void publish_blankWebhookUrl_skipsDeliveryAndDoesNotSave() {
        ReflectionTestUtils.setField(webhookPublisherService, "webhookUrl", "");

        webhookPublisherService.publish(testPayload("INVOICE_ISSUED"));

        verify(eventLogRepository, never()).save(any());
    }

    @Test
    void publish_onConnectionFailure_marksEventAsFailed() {
        // Return a mutable log so the service can update its status
        WebhookEventLog mutableLog = pendingLog("DELAY_REPORTED");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutableLog);

        webhookPublisherService.publish(testPayload("DELAY_REPORTED"));

        // After failed delivery, status should be FAILED (not PENDING and not DELIVERED)
        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeast(2)).save(captor.capture());

        WebhookEventLog lastSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSave.getStatus()).isIn("FAILED", "DEAD_LETTER");
    }

    @Test
    void publish_onConnectionFailure_incrementsAttemptCount() {
        WebhookEventLog mutableLog = pendingLog("PHASE_UPDATED");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutableLog);

        webhookPublisherService.publish(testPayload("PHASE_UPDATED"));

        // Attempts must be incremented beyond 0
        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeast(2)).save(captor.capture());

        WebhookEventLog lastSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSave.getAttempts()).isGreaterThan(0);
    }

    @Test
    void publish_onConnectionFailure_setsErrorMessage() {
        WebhookEventLog mutableLog = pendingLog("DOCUMENT_UPLOADED");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutableLog);

        webhookPublisherService.publish(testPayload("DOCUMENT_UPLOADED"));

        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeast(2)).save(captor.capture());

        WebhookEventLog lastSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSave.getErrorMessage()).isNotBlank();
    }

    // ── retryFailedEvents ─────────────────────────────────────────────────────

    @Test
    void retryFailedEvents_failedEventsUnderMaxAttempts_attemptsDelivery() {
        WebhookEventLog failedLog = pendingLog("INVOICE_PAID");
        failedLog.setStatus("FAILED");
        failedLog.setAttempts(2);

        when(eventLogRepository.findByStatusAndAttemptsLessThan("FAILED", 5))
                .thenReturn(List.of(failedLog));
        when(eventLogRepository.save(any())).thenReturn(failedLog);

        webhookPublisherService.retryFailedEvents();

        // The log must be saved after the retry attempt
        verify(eventLogRepository, atLeastOnce()).save(any(WebhookEventLog.class));
    }

    @Test
    void retryFailedEvents_noRetryableEvents_savesNothing() {
        when(eventLogRepository.findByStatusAndAttemptsLessThan("FAILED", 5))
                .thenReturn(List.of());

        webhookPublisherService.retryFailedEvents();

        verify(eventLogRepository, never()).save(any());
    }

    // ── publishHandoverShifted (S3 PR3) ───────────────────────────────────────

    @Test
    void publishHandoverShifted_buildsPayloadWithSignedShiftAndDirectionMetadata() {
        WebhookEventLog mutable = pendingLog("HANDOVER_SHIFT");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutable);

        webhookPublisherService.publishHandoverShifted(42L,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 8), 5);

        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeastOnce()).save(captor.capture());
        WebhookEventLog persisted = captor.getAllValues().get(0);

        assertThat(persisted.getEventType()).isEqualTo("HANDOVER_SHIFT");
        assertThat(persisted.getProjectId()).isEqualTo(42L);
        assertThat(persisted.getReferenceId()).isEqualTo(42L);
        assertThat(persisted.getPayload()).contains("\"shiftWorkingDays\":\"5\"");
        assertThat(persisted.getPayload()).contains("\"direction\":\"later\"");
        assertThat(persisted.getPayload()).contains("approximately 5 working days later");
    }

    @Test
    void publishHandoverShifted_negativeShift_setsEarlierDirection() {
        WebhookEventLog mutable = pendingLog("HANDOVER_SHIFT");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutable);

        webhookPublisherService.publishHandoverShifted(42L,
                LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 1), -5);

        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeastOnce()).save(captor.capture());
        WebhookEventLog persisted = captor.getAllValues().get(0);

        assertThat(persisted.getPayload()).contains("\"direction\":\"earlier\"");
        assertThat(persisted.getPayload()).contains("\"shiftWorkingDays\":\"-5\"");
        assertThat(persisted.getPayload()).contains("approximately 5 working days earlier");
    }

    @Test
    void publishHandoverShifted_singularDay_dropsTrailingS() {
        WebhookEventLog mutable = pendingLog("HANDOVER_SHIFT");
        when(eventLogRepository.save(any(WebhookEventLog.class))).thenReturn(mutable);

        webhookPublisherService.publishHandoverShifted(42L,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), 1);

        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPayload())
                .contains("approximately 1 working day later");
    }

    @Test
    void retryFailedEvents_eventAtMaxAttempts_promotesToDeadLetter() {
        // Simulate a log that will reach MAX_ATTEMPTS (5) after this retry attempt
        WebhookEventLog failedLog = pendingLog("PAYMENT_RECORDED");
        failedLog.setStatus("FAILED");
        failedLog.setAttempts(4); // will become 5 after increment → DLQ

        when(eventLogRepository.findByStatusAndAttemptsLessThan("FAILED", 5))
                .thenReturn(List.of(failedLog));
        when(eventLogRepository.save(any())).thenReturn(failedLog);

        webhookPublisherService.retryFailedEvents();

        // After the retry, the log should be in DEAD_LETTER (connection refused → attempts=5)
        ArgumentCaptor<WebhookEventLog> captor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(eventLogRepository, atLeastOnce()).save(captor.capture());

        WebhookEventLog saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("DEAD_LETTER");
    }
}
