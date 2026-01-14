package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for project type template with associated milestones
 */
public class ProjectTypeTemplateDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("project_type")
    private String projectType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("milestone_templates")
    private List<MilestoneTemplateDTO> milestoneTemplates;

    @JsonProperty("total_milestones")
    private Integer totalMilestones;

    @JsonProperty("estimated_total_days")
    private Integer estimatedTotalDays;

    // Constructors
    public ProjectTypeTemplateDTO() {
    }

    public ProjectTypeTemplateDTO(String projectType, String description, String category) {
        this.projectType = projectType;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<MilestoneTemplateDTO> getMilestoneTemplates() {
        return milestoneTemplates;
    }

    public void setMilestoneTemplates(List<MilestoneTemplateDTO> milestoneTemplates) {
        this.milestoneTemplates = milestoneTemplates;
    }

    public Integer getTotalMilestones() {
        return totalMilestones;
    }

    public void setTotalMilestones(Integer totalMilestones) {
        this.totalMilestones = totalMilestones;
    }

    public Integer getEstimatedTotalDays() {
        return estimatedTotalDays;
    }

    public void setEstimatedTotalDays(Integer estimatedTotalDays) {
        this.estimatedTotalDays = estimatedTotalDays;
    }
}

