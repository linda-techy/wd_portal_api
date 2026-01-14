package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Leads module
 * Extends base SearchFilterRequest with lead-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadSearchFilter extends SearchFilterRequest {
    
    // Lead-specific filters
    private String source;           // Lead source (website, referral, etc.)
    private String priority;         // Priority level (high, medium, low)
    private String customerType;     // Customer type (individual, commercial, etc.)
    private String projectType;      // Project type
    private String assignedTeam;     // Assigned team member
    private String state;            // Location - State
    private String district;         // Location - District
    
    // Budget range
    private Double minBudget;
    private Double maxBudget;
    
    /**
     * Check if budget range filter is applied
     */
    public boolean hasBudgetRange() {
        return minBudget != null || maxBudget != null;
    }
    
    /**
     * Check if location filter is applied
     */
    public boolean hasLocation() {
        return (state != null && !state.trim().isEmpty()) || 
               (district != null && !district.trim().isEmpty());
    }
}

