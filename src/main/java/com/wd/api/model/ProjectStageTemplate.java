package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One stage row in a per-project payment-stage template (audit P2-4).
 *
 * Percentages are stored as decimal fractions (0.10 = 10%).
 * All stages for a project must sum to 1.0 — enforced by the service layer
 * ({@link com.wd.api.service.ProjectStageTemplateService#setTemplate}).
 *
 * At BOQ approval time {@link com.wd.api.service.BoqDocumentService#recordCustomerApproval}
 * uses these rows as the default stage plan when no explicit overrides are provided.
 */
@Entity
@Table(name = "project_stage_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "stage_number"}))
public class ProjectStageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Decimal fraction (e.g. 0.10 = 10%). Must be > 0 and <= 1. */
    @Column(name = "percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal percentage;

    @Column(name = "milestone_description", columnDefinition = "TEXT")
    private String milestoneDescription;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProjectStageTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public Integer getStageNumber() { return stageNumber; }
    public void setStageNumber(Integer stageNumber) { this.stageNumber = stageNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public String getMilestoneDescription() { return milestoneDescription; }
    public void setMilestoneDescription(String milestoneDescription) {
        this.milestoneDescription = milestoneDescription;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
