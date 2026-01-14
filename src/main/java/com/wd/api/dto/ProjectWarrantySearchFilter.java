package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Project Warranties module
 * Extends base SearchFilterRequest with warranty-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectWarrantySearchFilter extends SearchFilterRequest {
    
    // Project Warranty-specific filters
    private Long projectId;          // Project ID
    private String warrantyType;     // Type (WORKMANSHIP, MATERIAL, COMPREHENSIVE)
    private Boolean activeOnly;      // Show only active warranties
    private Boolean expiredOnly;     // Show only expired warranties
    private Boolean expiringWithinDays; // Expiring within X days
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if active only filter is applied
     */
    public boolean isActiveOnly() {
        return activeOnly != null && activeOnly;
    }
}

