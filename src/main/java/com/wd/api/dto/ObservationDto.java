package com.wd.api.dto;

import com.wd.api.model.Observation;

import java.time.LocalDateTime;

/**
 * DTO for Observation (Snag) entity.
 * Used for API responses to avoid exposing internal entity details.
 */
public class ObservationDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private String location;
    private String priority;
    private String severity;
    private String status;
    private String imagePath;
    private String imageUrl;
    private LocalDateTime reportedDate;
    private Long reportedById;
    private String reportedByName;
    private String reportedByRole;
    private String resolutionNotes;
    private LocalDateTime resolvedDate;
    private Long resolvedById;
    private String resolvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ObservationDto() {
    }

    public static ObservationDto fromEntity(Observation entity) {
        ObservationDto dto = new ObservationDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setPriority(entity.getPriority());
        dto.setSeverity(entity.getSeverity());
        dto.setStatus(entity.getStatus());
        dto.setImagePath(entity.getImagePath());
        dto.setReportedDate(entity.getReportedDate());
        dto.setResolutionNotes(entity.getResolutionNotes());
        dto.setResolvedDate(entity.getResolvedDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Generate image URL from path
        if (entity.getImagePath() != null) {
            dto.setImageUrl("/api/files/download/" + entity.getImagePath());
        }

        if (entity.getProject() != null) {
            dto.setProjectId(entity.getProject().getId());
            dto.setProjectName(entity.getProject().getName());
        }

        if (entity.getReportedBy() != null) {
            dto.setReportedById(entity.getReportedBy().getId());
            dto.setReportedByName(entity.getReportedBy().getFirstName() + " " + entity.getReportedBy().getLastName());
        }

        if (entity.getReportedByRole() != null) {
            dto.setReportedByRole(entity.getReportedByRole().getName());
        }

        if (entity.getResolvedBy() != null) {
            dto.setResolvedById(entity.getResolvedBy().getId());
            dto.setResolvedByName(entity.getResolvedBy().getFirstName() + " " + entity.getResolvedBy().getLastName());
        }

        return dto;
    }

    /**
     * Creates a customer-facing DTO that hides sensitive internal data.
     */
    public static ObservationDto fromEntityForCustomer(Observation entity) {
        ObservationDto dto = fromEntity(entity);
        // Hide internal resolution notes from customer view
        dto.setResolutionNotes(null);
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(LocalDateTime reportedDate) {
        this.reportedDate = reportedDate;
    }

    public Long getReportedById() {
        return reportedById;
    }

    public void setReportedById(Long reportedById) {
        this.reportedById = reportedById;
    }

    public String getReportedByName() {
        return reportedByName;
    }

    public void setReportedByName(String reportedByName) {
        this.reportedByName = reportedByName;
    }

    public String getReportedByRole() {
        return reportedByRole;
    }

    public void setReportedByRole(String reportedByRole) {
        this.reportedByRole = reportedByRole;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDateTime getResolvedDate() {
        return resolvedDate;
    }

    public void setResolvedDate(LocalDateTime resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public Long getResolvedById() {
        return resolvedById;
    }

    public void setResolvedById(Long resolvedById) {
        this.resolvedById = resolvedById;
    }

    public String getResolvedByName() {
        return resolvedByName;
    }

    public void setResolvedByName(String resolvedByName) {
        this.resolvedByName = resolvedByName;
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
}
