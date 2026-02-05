package com.wd.api.dto;

import com.wd.api.model.GalleryImage;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for GalleryImage entity.
 * Used for API responses to avoid exposing internal entity details.
 */
public class GalleryImageDto {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long siteReportId;
    private String imageUrl;
    private String imagePath;
    private String thumbnailPath;
    private String caption;
    private String locationTag;
    private String[] tags;
    private LocalDate takenDate;
    private LocalDateTime uploadedAt;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime createdAt;

    public GalleryImageDto() {
    }

    public static GalleryImageDto fromEntity(GalleryImage entity) {
        GalleryImageDto dto = new GalleryImageDto();
        dto.setId(entity.getId());
        dto.setImageUrl(entity.getImageUrl());
        dto.setImagePath(entity.getImagePath());
        dto.setThumbnailPath(entity.getThumbnailPath());
        dto.setCaption(entity.getCaption());
        dto.setLocationTag(entity.getLocationTag());
        dto.setTags(entity.getTags());
        dto.setTakenDate(entity.getTakenDate());
        dto.setUploadedAt(entity.getUploadedAt());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getProject() != null) {
            dto.setProjectId(entity.getProject().getId());
            dto.setProjectName(entity.getProject().getName());
        }

        if (entity.getSiteReport() != null) {
            dto.setSiteReportId(entity.getSiteReport().getId());
        }

        if (entity.getUploadedBy() != null) {
            dto.setUploadedById(entity.getUploadedBy().getId());
            dto.setUploadedByName(entity.getUploadedBy().getFirstName() + " " + entity.getUploadedBy().getLastName());
        }

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

    public Long getSiteReportId() {
        return siteReportId;
    }

    public void setSiteReportId(Long siteReportId) {
        this.siteReportId = siteReportId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getLocationTag() {
        return locationTag;
    }

    public void setLocationTag(String locationTag) {
        this.locationTag = locationTag;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public LocalDate getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(LocalDate takenDate) {
        this.takenDate = takenDate;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(Long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
