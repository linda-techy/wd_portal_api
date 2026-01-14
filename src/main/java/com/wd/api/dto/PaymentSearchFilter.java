package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * Search and filter request for Payments module
 * Extends base SearchFilterRequest with payment-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentSearchFilter extends SearchFilterRequest {
    
    // Payment-specific filters
    private String paymentType;      // Payment type (CUSTOMER, VENDOR, CONTRACTOR, etc.)
    private String paymentMode;      // Payment mode (CASH, CHEQUE, BANK_TRANSFER, etc.)
    private Long projectId;          // Project ID
    private Long customerId;         // Customer ID
    private String invoiceNumber;    // Invoice number (partial match)
    private String transactionRef;   // Transaction reference (partial match)
    
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
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if payment type filter is applied
     */
    public boolean hasPaymentType() {
        return paymentType != null && !paymentType.trim().isEmpty();
    }
}

