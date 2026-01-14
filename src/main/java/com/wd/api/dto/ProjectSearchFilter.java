package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Customer Projects module
 * Extends base SearchFilterRequest with project-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectSearchFilter extends SearchFilterRequest {
    
    // Project-specific filters
    private String phase;            // Project phase (DESIGN, PLANNING, EXECUTION, etc.)
    private String type;             // Project type (RESIDENTIAL_VILLA, COMMERCIAL_COMPLEX, etc.)
    private String contractType;     // Contract type (FIXED_PRICE, TIME_MATERIAL, etc.)
    private Long managerId;          // Project manager ID
    private Long customerId;         // Customer ID
    private String location;         // Project location
    private String city;             // City
    private String state;            // State
    
    // Budget/Amount range
    private Double minBudget;
    private Double maxBudget;
    
    // Progress range
    private Double minProgress;
    private Double maxProgress;
    
    /**
     * Check if budget range filter is applied
     */
    public boolean hasBudgetRange() {
        return minBudget != null || maxBudget != null;
    }
    
    /**
     * Check if progress range filter is applied
     */
    public boolean hasProgressRange() {
        return minProgress != null || maxProgress != null;
    }
    
    /**
     * Check if location filter is applied
     */
    public boolean hasLocation() {
        return (location != null && !location.trim().isEmpty()) ||
               (city != null && !city.trim().isEmpty()) ||
               (state != null && !state.trim().isEmpty());
    }
}

