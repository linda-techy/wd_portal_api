package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Subcontracts module
 * Extends base SearchFilterRequest with subcontract-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubcontractSearchFilter extends SearchFilterRequest {
    
    // Subcontract-specific filters
    private Long projectId;          // Project ID
    private String workOrderNumber;  // Work order number (partial match)
    private String contractorName;   // Contractor name (partial match)
    private Long contractorId;       // Contractor ID
    private String workType;         // Type of work
    
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
     * Check if contractor filter is applied
     */
    public boolean hasContractor() {
        return contractorName != null && !contractorName.trim().isEmpty();
    }
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
}

