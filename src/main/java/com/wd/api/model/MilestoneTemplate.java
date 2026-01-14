package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing milestone templates for different project types
 * Defines default milestones with percentages and phases
 */
@Entity
@Table(name = "milestone_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "milestone_order"}))
public class MilestoneTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProjectTypeTemplate template;

    @Column(name = "milestone_name", nullable = false)
    private String milestoneName;

    @Column(name = "milestone_order", nullable = false)
    private Integer milestoneOrder;

    @Column(name = "default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultPercentage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "phase", length = 50)
    private String phase; // DESIGN, PLANNING, EXECUTION, COMPLETION, HANDOVER, WARRANTY

    @Column(name = "estimated_duration_days")
    private Integer estimatedDurationDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public MilestoneTemplate() {
        this.createdAt = LocalDateTime.now();
    }

    public MilestoneTemplate(ProjectTypeTemplate template, String milestoneName, Integer milestoneOrder,
                           BigDecimal defaultPercentage, String description, String phase) {
        this();
        this.template = template;
        this.milestoneName = milestoneName;
        this.milestoneOrder = milestoneOrder;
        this.defaultPercentage = defaultPercentage;
        this.description = description;
        this.phase = phase;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectTypeTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ProjectTypeTemplate template) {
        this.template = template;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public Integer getMilestoneOrder() {
        return milestoneOrder;
    }

    public void setMilestoneOrder(Integer milestoneOrder) {
        this.milestoneOrder = milestoneOrder;
    }

    public BigDecimal getDefaultPercentage() {
        return defaultPercentage;
    }

    public void setDefaultPercentage(BigDecimal defaultPercentage) {
        this.defaultPercentage = defaultPercentage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Integer getEstimatedDurationDays() {
        return estimatedDurationDays;
    }

    public void setEstimatedDurationDays(Integer estimatedDurationDays) {
        this.estimatedDurationDays = estimatedDurationDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

