package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wd.api.model.Document;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String filename;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("download_url")
    private String downloadUrl;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("file_type")
    private String fileType;

    private String description;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("reference_type")
    private String referenceType;

    @JsonProperty("uploaded_by_id")
    private Long uploadedById;

    @JsonProperty("uploaded_by_name")
    private String uploadedByName;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public DocumentResponse() {
    }

    public DocumentResponse(Document doc, String uploaderName) {
        this.id = doc.getId();
        this.filename = doc.getFilename();
        this.filePath = doc.getFilePath();
        this.downloadUrl = "/api/storage/" + doc.getFilePath();
        this.fileSize = doc.getFileSize();
        this.fileType = doc.getFileType();
        this.description = doc.getDescription();
        if (doc.getCategory() != null) {
            this.categoryId = doc.getCategory().getId();
            this.categoryName = doc.getCategory().getName();
        }
        this.referenceId = doc.getReferenceId();
        this.referenceType = doc.getReferenceType();
        this.uploadedById = doc.getCreatedByUserId();
        this.uploadedByName = uploaderName;
        this.createdAt = doc.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
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
