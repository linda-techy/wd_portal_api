package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for BOQ (Bill of Quantities) module
 * Extends base SearchFilterRequest with BOQ-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BoqSearchFilter extends SearchFilterRequest {
    
    // BOQ-specific filters
    private Long projectId;          // Project ID
    private Long workTypeId;         // Work type ID
    private String itemCode;         // Item code (partial match)
    private Boolean active;          // Active/Inactive items
    
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
     * Check if work type filter is applied
     */
    public boolean hasWorkType() {
        return workTypeId != null;
    }
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
}

