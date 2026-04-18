package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event_log")
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public WebhookEventLog() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public Long getProjectId() { return projectId; }
    public Long getCustomerId() { return customerId; }
    public Long getReferenceId() { return referenceId; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public Integer getAttempts() { return attempts; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setStatus(String status) { this.status = status; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
