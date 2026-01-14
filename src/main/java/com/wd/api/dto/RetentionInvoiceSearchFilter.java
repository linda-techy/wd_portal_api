package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Retention Invoices module
 * Extends base SearchFilterRequest with retention-invoice-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RetentionInvoiceSearchFilter extends SearchFilterRequest {
    
    // Retention Invoice-specific filters
    private Long projectId;          // Project ID
    private String invoiceNumber;    // Invoice number (partial match)
    private String releaseStatus;    // Release status (PENDING, PARTIAL, RELEASED)
    
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

