package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Project Variations module
 * Extends base SearchFilterRequest with variation-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectVariationSearchFilter extends SearchFilterRequest {
    
    // Project Variation-specific filters
    private Long projectId;          // Project ID
    private String variationType;    // Type (ADDITION, DELETION, MODIFICATION)
    private String approvalStatus;   // Approval status
    private Long requestedById;      // User who requested the variation
    private Long approvedById;       // User who approved the variation
    
    // Amount range
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
}

