package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Site Visits module
 * Extends base SearchFilterRequest with site-visit-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SiteVisitSearchFilter extends SearchFilterRequest {
    
    // Site Visit-specific filters
    private Long projectId;          // Project ID
    private Long visitedById;        // Visitor user ID
    private String visitType;        // Visit type (SITE_ENGINEER, PROJECT_MANAGER, etc.)
    private String visitStatus;      // Visit status (CHECKED_IN, CHECKED_OUT, etc.)
    private Boolean activeOnly;      // Show only currently active visits
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if visitor filter is applied
     */
    public boolean hasVisitor() {
        return visitedById != null;
    }
    
    /**
     * Check if visit type filter is applied
     */
    public boolean hasVisitType() {
        return visitType != null && !visitType.trim().isEmpty();
    }
    
    /**
     * Check if active only filter is applied
     */
    public boolean isActiveOnly() {
        return activeOnly != null && activeOnly;
    }
}

