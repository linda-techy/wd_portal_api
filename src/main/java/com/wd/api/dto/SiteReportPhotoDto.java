package com.wd.api.dto;

import com.wd.api.model.SiteReportPhoto;
import java.time.LocalDateTime;

public class SiteReportPhotoDto {
    private Long id;
    private String photoUrl;
    private String storagePath;
    private LocalDateTime createdAt;
    private String caption;
    private Double latitude;
    private Double longitude;
    private Integer displayOrder;

    public SiteReportPhotoDto(SiteReportPhoto photo) {
        this.id = photo.getId();
        this.photoUrl = photo.getPhotoUrl();
        this.storagePath = photo.getStoragePath();
        this.createdAt = photo.getCreatedAt();
        this.caption = photo.getCaption();
        this.latitude = photo.getLatitude();
        this.longitude = photo.getLongitude();
        this.displayOrder = photo.getDisplayOrder();
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
