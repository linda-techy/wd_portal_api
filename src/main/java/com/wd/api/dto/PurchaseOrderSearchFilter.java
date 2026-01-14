package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Purchase Orders module
 * Extends base SearchFilterRequest with PO-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderSearchFilter extends SearchFilterRequest {
    
    // PO-specific filters
    private Long vendorId;           // Vendor ID
    private Long projectId;          // Project ID
    private String poNumber;         // PO number (exact match or partial)
    
    // Amount range
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
    
    /**
     * Check if vendor filter is applied
     */
    public boolean hasVendor() {
        return vendorId != null;
    }
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
}

