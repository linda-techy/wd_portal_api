package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to the customer_notifications table owned by customer_api (created in V1001).
 * Portal API writes notifications here when portal-side events affect customers
 * (site reports, payments, milestones, documents).
 */
@Entity
@Table(name = "customer_notifications")
public class CustomerNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private CustomerUser customerUser;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // SITE_REPORT, PAYMENT, BOQ, DOCUMENT, MILESTONE, GENERAL
    @Column(name = "notification_type", length = 50)
    private String notificationType;

    // ID of the linked entity (e.g. site_report_id, payment_schedule_id)
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors

    public CustomerNotification() {}

    public CustomerNotification(CustomerUser customerUser, Long projectId,
                                 String title, String body,
                                 String notificationType, Long referenceId) {
        this.customerUser = customerUser;
        this.projectId = projectId;
        this.title = title;
        this.body = body;
        this.notificationType = notificationType;
        this.referenceId = referenceId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerUser getCustomerUser() { return customerUser; }
    public void setCustomerUser(CustomerUser customerUser) { this.customerUser = customerUser; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
