package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Lead Quotations module
 * Extends base SearchFilterRequest with quotation-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadQuotationSearchFilter extends SearchFilterRequest {
    
    // Lead Quotation-specific filters
    private Long leadId;             // Lead ID
    private String quotationNumber;  // Quotation number (partial match)
    private Long preparedById;       // User who prepared the quotation
    private String validityStatus;   // Status (VALID, EXPIRED, ACCEPTED, REJECTED)
    
    // Amount range
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
    /**
     * Check if lead filter is applied
     */
    public boolean hasLead() {
        return leadId != null;
    }
    
    /**
     * Check if amount range filter is applied
     */
    public boolean hasAmountRange() {
        return minAmount != null || maxAmount != null;
    }
}

