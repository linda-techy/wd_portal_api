package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO for milestone template information
 * Used to display available milestone templates for project types
 */
public class MilestoneTemplateDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("template_id")
    private Long templateId;

    @JsonProperty("milestone_name")
    private String milestoneName;

    @JsonProperty("milestone_order")
    private Integer milestoneOrder;

    @JsonProperty("default_percentage")
    private BigDecimal defaultPercentage;

    @JsonProperty("description")
    private String description;

    @JsonProperty("phase")
    private String phase;

    @JsonProperty("estimated_duration_days")
    private Integer estimatedDurationDays;

    // Constructors
    public MilestoneTemplateDTO() {
    }

    public MilestoneTemplateDTO(String milestoneName, Integer milestoneOrder, BigDecimal defaultPercentage,
                              String description, String phase) {
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
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
}

