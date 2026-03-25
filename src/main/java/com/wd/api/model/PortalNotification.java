package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persisted notification record for portal staff users.
 * Stored in portal_notifications table (created by V6 migration).
 *
 * notification_type values:
 *   LEAD_NEW      — new lead submitted
 *   LEAD_ASSIGNED — lead assigned to this staff member
 *   TASK_ASSIGNED — task assigned to this staff member
 *   TASK_OVERDUE  — task past due date
 *   GENERAL       — ad-hoc notification
 */
@Entity
@Table(name = "portal_notifications")
public class PortalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portal_user_id", nullable = false)
    private PortalUser portalUser;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "notification_type", length = 50)
    private String notificationType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PortalUser getPortalUser() { return portalUser; }
    public void setPortalUser(PortalUser portalUser) { this.portalUser = portalUser; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

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
