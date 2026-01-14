package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Partnerships module
 * Extends base SearchFilterRequest with partnership-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnershipSearchFilter extends SearchFilterRequest {
    
    // Partnership-specific filters
    private Long projectId;          // Project ID
    private String partnershipType;  // Type (JOINT_VENTURE, CONSORTIUM, SUBCONTRACT, etc.)
    private Long partnerId;          // Partner company/user ID
    private String partnerName;      // Partner name (partial match)
    private Boolean activeOnly;      // Show only active partnerships
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if partner name filter is applied
     */
    public boolean hasPartnerName() {
        return partnerName != null && !partnerName.trim().isEmpty();
    }
}

