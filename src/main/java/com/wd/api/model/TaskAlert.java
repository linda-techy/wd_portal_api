package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Task Alert Entity - Production-Grade Alert System
 * 
 * Business Purpose (Construction Domain):
 * - Tracks all deadline alerts sent to admins/managers
 * - Prevents duplicate alert spam (24-hour deduplication)
 * - Provides audit trail for compliance
 * - Enables analytics on overdue task trends
 * 
 * Alert Severity Levels:
 * - CRITICAL: Overdue tasks (past due date)
 * - HIGH: Tasks due today
 * - MEDIUM: Tasks due within 3 days (early warning)
 * 
 * @author Senior Engineer (15+ years construction experience)
 */
@Entity
@Table(name = "task_alerts")
public class TaskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "alert_message", columnDefinition = "TEXT", nullable = false)
    private String alertMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_to_user_id")
    private User sentToUser;

    @Column(name = "sent_to_email", length = 255)
    private String sentToEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Enums
    public enum AlertType {
        OVERDUE, // Task is past due date (CRITICAL)
        DUE_TODAY, // Task due today (HIGH)
        DUE_SOON // Task due within 3 days (MEDIUM)
    }

    public enum AlertSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum DeliveryStatus {
        SENT, // Successfully delivered
        FAILED, // Delivery failed (retry needed)
        PENDING // Queued for delivery
    }

    // Constructors
    public TaskAlert() {
    }

    public TaskAlert(Task task, AlertType alertType, AlertSeverity severity,
            String alertMessage, User sentToUser, String sentToEmail) {
        this.task = task;
        this.alertType = alertType;
        this.severity = severity;
        this.alertMessage = alertMessage;
        this.sentAt = LocalDateTime.now();
        this.sentToUser = sentToUser;
        this.sentToEmail = sentToEmail;
        this.deliveryStatus = DeliveryStatus.SENT;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public User getSentToUser() {
        return sentToUser;
    }

    public void setSentToUser(User sentToUser) {
        this.sentToUser = sentToUser;
    }

    public String getSentToEmail() {
        return sentToEmail;
    }

    public void setSentToEmail(String sentToEmail) {
        this.sentToEmail = sentToEmail;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
