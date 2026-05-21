package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One ITP (Inspection-Test Plan) gate on a schedule task.
 * Exactly three rows exist per task: PRELIMINARY, IN_PROGRESS, FINAL.
 * See V152__task_quality_gates.sql for table contract.
 */
@Entity
@Table(name = "task_quality_gates")
public class TaskQualityGate {

    public enum GateType { PRELIMINARY, IN_PROGRESS, FINAL }
    public enum Status   { PENDING, PASSED, FAILED, NA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false, length = 20)
    private GateType gateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id")
    private PortalUser signedBy;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── getters / setters ─────────────────────────────────────────────────
    public Long getId() { return id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public GateType getGateType() { return gateType; }
    public void setGateType(GateType gateType) { this.gateType = gateType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public PortalUser getSignedBy() { return signedBy; }
    public void setSignedBy(PortalUser signedBy) { this.signedBy = signedBy; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
