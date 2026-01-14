package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Vendor Quotations module
 * Extends base SearchFilterRequest with quotation-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VendorQuotationSearchFilter extends SearchFilterRequest {
    
    // Vendor Quotation-specific filters
    private Long vendorId;           // Vendor ID
    private Long projectId;          // Project ID
    private Long indentId;           // Material Indent ID
    private String quotationNumber;  // Quotation number (partial match)
    
    // Amount range
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
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
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
}

