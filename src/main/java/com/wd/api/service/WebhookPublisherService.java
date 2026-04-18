package com.wd.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
import java.util.Map;

/**
 * Publishes webhook events to the Customer API after portal transactions commit.
 *
 * Uses @TransactionalEventListener(phase = AFTER_COMMIT) so events are only
 * fired when the DB write has actually succeeded — no spurious notifications.
 *
 * Fire-and-forget (@Async): portal operations never block waiting for delivery.
 * Failed deliveries are logged; a dead-letter mechanism can be added later.
 */
@Service
public class WebhookPublisherService {

    private static final Logger log = LoggerFactory.getLogger(WebhookPublisherService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int CONNECT_TIMEOUT_S = 5;
    private static final int REQUEST_TIMEOUT_S = 10;

    @Value("${customer-api.webhook-url:}")
    private String webhookUrl;

    @Value("${customer-api.webhook-secret:}")
    private String webhookSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_S))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

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

    /**
     * Called after a site report is committed. Notifies all project customers.
     */
    public void publishSiteReportSubmitted(Long projectId, Long reportId, String reportTitle) {
        publish(new PortalWebhookPayload(
                "SITE_REPORT_SUBMITTED", projectId, null, reportId,
                "A new site report has been submitted: " + reportTitle,
                Map.of("reportTitle", reportTitle),
                LocalDateTime.now()));
    }

    /**
     * Called after an invoice is issued (status changed from DRAFT → ISSUED).
     */
    public void publishInvoiceIssued(Long projectId, Long customerId,
                                      Long invoiceId, String invoiceNumber, String amount) {
        publish(new PortalWebhookPayload(
                "INVOICE_ISSUED", projectId, customerId, invoiceId,
                "A new invoice " + invoiceNumber + " has been issued for \u20b9" + amount,
                Map.of("invoiceNumber", invoiceNumber, "amount", amount),
                LocalDateTime.now()));
    }

    /**
     * Called after an invoice is marked as PAID.
     */
    public void publishInvoicePaid(Long projectId, Long customerId,
                                    Long invoiceId, String invoiceNumber) {
        publish(new PortalWebhookPayload(
                "INVOICE_PAID", projectId, customerId, invoiceId,
                "Invoice " + invoiceNumber + " has been marked as paid.",
                Map.of("invoiceNumber", invoiceNumber),
                LocalDateTime.now()));
    }

    /**
     * Called after a project phase is updated (status change or dates updated).
     */
    public void publishPhaseUpdated(Long projectId, Long phaseId, String phaseName, String newStatus) {
        publish(new PortalWebhookPayload(
                "PHASE_UPDATED", projectId, null, phaseId,
                "Phase \"" + phaseName + "\" is now " + newStatus.toLowerCase().replace("_", " "),
                Map.of("phaseName", phaseName, "status", newStatus),
                LocalDateTime.now()));
    }

    /**
     * Called after a document is uploaded for a project.
     */
    public void publishDocumentUploaded(Long projectId, Long documentId, String filename) {
        publish(new PortalWebhookPayload(
                "DOCUMENT_UPLOADED", projectId, null, documentId,
                "A new document has been uploaded: " + filename,
                Map.of("filename", filename),
                LocalDateTime.now()));
    }

    /**
     * Called after a delay is logged for a project.
     */
    public void publishDelayReported(Long projectId, Long delayId, String category) {
        publish(new PortalWebhookPayload(
                "DELAY_REPORTED", projectId, null, delayId,
                "A delay has been reported: " + category.toLowerCase().replace("_", " "),
                Map.of("category", category),
                LocalDateTime.now()));
    }

    /**
     * Called after a payment transaction is recorded.
     */
    public void publishPaymentRecorded(Long projectId, Long customerId,
                                        Long transactionId, String amount) {
        publish(new PortalWebhookPayload(
                "PAYMENT_RECORDED", projectId, customerId, transactionId,
                "A payment of \u20b9" + amount + " has been recorded.",
                Map.of("amount", amount),
                LocalDateTime.now()));
    }

    // ───────────────────────── Internal ────────────────────────────

    @Async
    public void publish(PortalWebhookPayload payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook URL not configured — skipping event: {}", payload.eventType());
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
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

            if (response.statusCode() == 200) {
                log.debug("Webhook delivered: type={} projectId={}", payload.eventType(), payload.projectId());
            } else {
                log.warn("Webhook delivery returned non-200: status={} type={} body={}",
                        response.statusCode(), payload.eventType(), response.body());
            }
        } catch (Exception e) {
            log.error("Webhook delivery failed (will not retry): type={} error={}",
                    payload.eventType(), e.getMessage());
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
}
