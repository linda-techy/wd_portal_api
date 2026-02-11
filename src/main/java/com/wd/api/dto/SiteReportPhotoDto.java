package com.wd.api.dto;

import com.wd.api.model.SiteReportPhoto;
import java.time.LocalDateTime;

public class SiteReportPhotoDto {
    private Long id;
    private String photoUrl;
    private String storagePath;
    private LocalDateTime createdAt;

    public SiteReportPhotoDto(SiteReportPhoto photo) {
        this.id = photo.getId();
        this.photoUrl = photo.getPhotoUrl();
        this.storagePath = photo.getStoragePath();
        this.createdAt = photo.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
