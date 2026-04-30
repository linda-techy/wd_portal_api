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

    /**
     * "Closing soon" — SENT or VIEWED quotations whose validity window
     * elapses within the next 7 days. Drives the urgent-pipeline chip on
     * the Flutter list screen so staff can pull up the deals that need
     * a follow-up call today.
     *
     * <p>When {@code true}, the service short-circuits the normal
     * Specification path and uses
     * {@link com.wd.api.repository.LeadQuotationRepository#findClosingSoon}
     * — Postgres-native date arithmetic that JPA Criteria can't express
     * cleanly across vendors.
     */
    private Boolean closingSoon;

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

