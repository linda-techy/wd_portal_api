package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BOQ Audit Log entity - immutable audit trail for all BOQ operations.
 * Records are never updated or deleted for compliance and traceability.
 */
@Entity
@Table(name = "boq_audit_logs")
public class BoqAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private CustomerProject project;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "old_value", columnDefinition = "JSONB")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSONB")
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private PortalUser changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    // ---- Constructor ----

    public BoqAuditLog() {}

    public BoqAuditLog(String entityType, Long entityId, CustomerProject project, String actionType,
                       String oldValue, String newValue, PortalUser changedBy) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.project = project;
        this.actionType = actionType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public PortalUser getChangedBy() { return changedBy; }
    public void setChangedBy(PortalUser changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
