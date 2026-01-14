package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for tracking project progress changes (audit trail)
 * Records all updates to project progress with reasons
 */
@Entity
@Table(name = "project_progress_logs")
public class ProjectProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "previous_progress", precision = 5, scale = 2)
    private BigDecimal previousProgress;

    @Column(name = "new_progress", precision = 5, scale = 2)
    private BigDecimal newProgress;

    @Column(name = "previous_milestone_progress", precision = 5, scale = 2)
    private BigDecimal previousMilestoneProgress;

    @Column(name = "new_milestone_progress", precision = 5, scale = 2)
    private BigDecimal newMilestoneProgress;

    @Column(name = "previous_task_progress", precision = 5, scale = 2)
    private BigDecimal previousTaskProgress;

    @Column(name = "new_task_progress", precision = 5, scale = 2)
    private BigDecimal newTaskProgress;

    @Column(name = "previous_budget_progress", precision = 5, scale = 2)
    private BigDecimal previousBudgetProgress;

    @Column(name = "new_budget_progress", precision = 5, scale = 2)
    private BigDecimal newBudgetProgress;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "change_type", length = 50)
    private String changeType; // MILESTONE_UPDATE, TASK_UPDATE, BUDGET_UPDATE, MANUAL_ADJUSTMENT, RECALCULATION

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private PortalUser changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Constructors
    public ProjectProgressLog() {
        this.changedAt = LocalDateTime.now();
    }

    public ProjectProgressLog(CustomerProject project, BigDecimal previousProgress, BigDecimal newProgress,
                            String changeType, String changeReason) {
        this();
        this.project = project;
        this.previousProgress = previousProgress;
        this.newProgress = newProgress;
        this.changeType = changeType;
        this.changeReason = changeReason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public BigDecimal getPreviousProgress() {
        return previousProgress;
    }

    public void setPreviousProgress(BigDecimal previousProgress) {
        this.previousProgress = previousProgress;
    }

    public BigDecimal getNewProgress() {
        return newProgress;
    }

    public void setNewProgress(BigDecimal newProgress) {
        this.newProgress = newProgress;
    }

    public BigDecimal getPreviousMilestoneProgress() {
        return previousMilestoneProgress;
    }

    public void setPreviousMilestoneProgress(BigDecimal previousMilestoneProgress) {
        this.previousMilestoneProgress = previousMilestoneProgress;
    }

    public BigDecimal getNewMilestoneProgress() {
        return newMilestoneProgress;
    }

    public void setNewMilestoneProgress(BigDecimal newMilestoneProgress) {
        this.newMilestoneProgress = newMilestoneProgress;
    }

    public BigDecimal getPreviousTaskProgress() {
        return previousTaskProgress;
    }

    public void setPreviousTaskProgress(BigDecimal previousTaskProgress) {
        this.previousTaskProgress = previousTaskProgress;
    }

    public BigDecimal getNewTaskProgress() {
        return newTaskProgress;
    }

    public void setNewTaskProgress(BigDecimal newTaskProgress) {
        this.newTaskProgress = newTaskProgress;
    }

    public BigDecimal getPreviousBudgetProgress() {
        return previousBudgetProgress;
    }

    public void setPreviousBudgetProgress(BigDecimal previousBudgetProgress) {
        this.previousBudgetProgress = previousBudgetProgress;
    }

    public BigDecimal getNewBudgetProgress() {
        return newBudgetProgress;
    }

    public void setNewBudgetProgress(BigDecimal newBudgetProgress) {
        this.newBudgetProgress = newBudgetProgress;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public PortalUser getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(PortalUser changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}

