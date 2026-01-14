package com.wd.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing project type templates with predefined characteristics
 * Used to auto-populate milestones based on project type selection
 */
@Entity
@Table(name = "project_type_templates")
public class ProjectTypeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_type", unique = true, nullable = false, length = 100)
    private String projectType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category; // RESIDENTIAL, COMMERCIAL, INFRASTRUCTURE, INTERIOR

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MilestoneTemplate> milestoneTemplates;

    // Constructors
    public ProjectTypeTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ProjectTypeTemplate(String projectType, String description, String category) {
        this();
        this.projectType = projectType;
        this.description = description;
        this.category = category;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MilestoneTemplate> getMilestoneTemplates() {
        return milestoneTemplates;
    }

    public void setMilestoneTemplates(List<MilestoneTemplate> milestoneTemplates) {
        this.milestoneTemplates = milestoneTemplates;
    }
}

