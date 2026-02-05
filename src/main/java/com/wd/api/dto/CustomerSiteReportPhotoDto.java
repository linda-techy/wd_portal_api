package com.wd.api.dto;

import com.wd.api.model.SiteReportPhoto;

/**
 * Customer-safe DTO for Site Report Photos.
 * Only includes photo URL, excludes internal metadata.
 */
public class CustomerSiteReportPhotoDto {

    private Long id;
    private String photoUrl;

    public CustomerSiteReportPhotoDto(SiteReportPhoto photo) {
        this.id = photo.getId();
        this.photoUrl = photo.getPhotoUrl();
        // Note: SiteReportPhoto schema doesn't have caption field
    }

    // Getters and Setters
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
}
