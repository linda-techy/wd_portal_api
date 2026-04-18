package com.wd.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.model.WebhookEventLog;
import com.wd.api.repository.WebhookEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Publishes webhook events to the Customer API after portal transactions commit.
 *
 * Every outgoing event is persisted to {@code webhook_event_log} before delivery.
 * On success, the log entry is marked DELIVERED. On failure it remains FAILED and
 * a @Scheduled job retries up to MAX_ATTEMPTS times every 5 minutes. Events that
 * exhaust all retries are marked DEAD_LETTER for manual inspection.
 */
@Service
public class WebhookPublisherService {

    private static final Logger log = LoggerFactory.getLogger(WebhookPublisherService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int CONNECT_TIMEOUT_S = 5;
    private static final int REQUEST_TIMEOUT_S = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Value("${customer-api.webhook-url:}")
    private String webhookUrl;

    @Value("${customer-api.webhook-secret:}")
    private String webhookSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_S))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final WebhookEventLogRepository eventLogRepository;

    public WebhookPublisherService(WebhookEventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    // ───────────────────────── Event Types ─────────────────────────

    public record PortalWebhookPayload(
            String eventType,
            Long projectId,
            Long customerId,
            Long referenceId,
            String summary,
            Map<String, String> metadata,
            LocalDateTime occurredAt
    ) {}

    // ───────────────────────── Public API ──────────────────────────

    public void publishSiteReportSubmitted(Long projectId, Long reportId, String reportTitle) {
        publish(new PortalWebhookPayload(
                "SITE_REPORT_SUBMITTED", projectId, null, reportId,
                "A new site report has been submitted: " + reportTitle,
                Map.of("reportTitle", reportTitle),
                LocalDateTime.now()));
    }

    public void publishInvoiceIssued(Long projectId, Long customerId,
                                      Long invoiceId, String invoiceNumber, String amount) {
        publish(new PortalWebhookPayload(
                "INVOICE_ISSUED", projectId, customerId, invoiceId,
                "A new invoice " + invoiceNumber + " has been issued for \u20b9" + amount,
                Map.of("invoiceNumber", invoiceNumber, "amount", amount),
                LocalDateTime.now()));
    }

    public void publishInvoicePaid(Long projectId, Long customerId,
                                    Long invoiceId, String invoiceNumber) {
        publish(new PortalWebhookPayload(
                "INVOICE_PAID", projectId, customerId, invoiceId,
                "Invoice " + invoiceNumber + " has been marked as paid.",
                Map.of("invoiceNumber", invoiceNumber),
                LocalDateTime.now()));
    }

    public void publishPhaseUpdated(Long projectId, Long phaseId, String phaseName, String newStatus) {
        publish(new PortalWebhookPayload(
                "PHASE_UPDATED", projectId, null, phaseId,
                "Phase \"" + phaseName + "\" is now " + newStatus.toLowerCase().replace("_", " "),
                Map.of("phaseName", phaseName, "status", newStatus),
                LocalDateTime.now()));
    }

    public void publishDocumentUploaded(Long projectId, Long documentId, String filename) {
        publish(new PortalWebhookPayload(
                "DOCUMENT_UPLOADED", projectId, null, documentId,
                "A new document has been uploaded: " + filename,
                Map.of("filename", filename),
                LocalDateTime.now()));
    }

    public void publishDelayReported(Long projectId, Long delayId, String category) {
        publish(new PortalWebhookPayload(
                "DELAY_REPORTED", projectId, null, delayId,
                "A delay has been reported: " + category.toLowerCase().replace("_", " "),
                Map.of("category", category),
                LocalDateTime.now()));
    }

    public void publishPaymentRecorded(Long projectId, Long customerId,
                                        Long transactionId, String amount) {
        publish(new PortalWebhookPayload(
                "PAYMENT_RECORDED", projectId, customerId, transactionId,
                "A payment of \u20b9" + amount + " has been recorded.",
                Map.of("amount", amount),
                LocalDateTime.now()));
    }

    // ───────────────────────── Internal ────────────────────────────

    /**
     * Persists the event to the audit log then attempts delivery asynchronously.
     */
    @Async
    @Transactional
    public void publish(PortalWebhookPayload payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook URL not configured — skipping event: {}", payload.eventType());
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload for event {}: {}", payload.eventType(), e.getMessage());
            return;
        }

        // Persist audit record before attempting delivery
        WebhookEventLog eventLog = new WebhookEventLog();
        eventLog.setEventType(payload.eventType());
        eventLog.setProjectId(payload.projectId());
        eventLog.setCustomerId(payload.customerId());
        eventLog.setReferenceId(payload.referenceId());
        eventLog.setPayload(json);
        eventLog.setStatus("PENDING");
        eventLog = eventLogRepository.save(eventLog);

        attemptDelivery(eventLog, json);
    }

    /**
     * Retries FAILED events every 5 minutes; promotes to DEAD_LETTER after MAX_ATTEMPTS.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void retryFailedEvents() {
        List<WebhookEventLog> retryable = eventLogRepository
                .findByStatusAndAttemptsLessThan("FAILED", MAX_ATTEMPTS);

        if (retryable.isEmpty()) return;

        log.info("Webhook retry job: found {} FAILED events to retry", retryable.size());
        for (WebhookEventLog eventLog : retryable) {
            attemptDelivery(eventLog, eventLog.getPayload());
        }
    }

    // ───────────────────────── Delivery logic ──────────────────────

    private void attemptDelivery(WebhookEventLog eventLog, String json) {
        try {
            String signature = computeSignature(json);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_S))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            if (signature != null) {
                requestBuilder.header("X-Portal-Signature", "sha256=" + signature);
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            eventLog.setAttempts(eventLog.getAttempts() + 1);
            eventLog.setLastAttemptAt(LocalDateTime.now());

            if (response.statusCode() == 200) {
                eventLog.setStatus("DELIVERED");
                eventLog.setDeliveredAt(LocalDateTime.now());
                eventLog.setErrorMessage(null);
                log.debug("Webhook delivered: type={} projectId={}", eventLog.getEventType(), eventLog.getProjectId());
            } else {
                String errMsg = "HTTP " + response.statusCode() + ": " + truncate(response.body(), 500);
                eventLog.setStatus("FAILED");
                eventLog.setErrorMessage(errMsg);
                log.warn("Webhook delivery returned non-200: status={} type={} attempt={}",
                        response.statusCode(), eventLog.getEventType(), eventLog.getAttempts());
                promoteToDlqIfExhausted(eventLog);
            }
        } catch (Exception e) {
            eventLog.setAttempts(eventLog.getAttempts() + 1);
            eventLog.setLastAttemptAt(LocalDateTime.now());
            eventLog.setStatus("FAILED");
            eventLog.setErrorMessage(truncate(e.getMessage(), 500));
            log.error("Webhook delivery failed: type={} attempt={} error={}",
                    eventLog.getEventType(), eventLog.getAttempts(), e.getMessage());
            promoteToDlqIfExhausted(eventLog);
        }
        eventLogRepository.save(eventLog);
    }

    private void promoteToDlqIfExhausted(WebhookEventLog eventLog) {
        if (eventLog.getAttempts() >= MAX_ATTEMPTS) {
            eventLog.setStatus("DEAD_LETTER");
            log.error("Webhook event moved to DEAD_LETTER after {} attempts: id={} type={}",
                    MAX_ATTEMPTS, eventLog.getId(), eventLog.getEventType());
        }
    }

    private String computeSignature(String payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Failed to compute webhook signature: {}", e.getMessage());
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
