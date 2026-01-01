package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Subcontract Work Order Summary DTO
 * Provides financial overview of a work order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractSummaryDTO {

    private Long workOrderId;
    private String workOrderNumber;
    private Long projectId;
    private String projectName;
    private Long vendorId;
    private String vendorName;
    private String scopeDescription;
    private String measurementBasis;
    private String status;

    // Financial Summary
    private BigDecimal totalContractAmount;
    private BigDecimal totalMeasuredAmount; // For unit-rate contracts
    private BigDecimal totalPaid;
    private BigDecimal totalTds;
    private BigDecimal balanceDue;

    // Progress Indicators
    private Integer totalMeasurements;
    private Integer approvedMeasurements;
    private Integer pendingMeasurements;
    private Integer totalPayments;

    // Percentage calculations
    private BigDecimal percentageCompleted;
    private BigDecimal percentagePaid;

    // Derived helpers
    public boolean isFullyPaid() {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean hasOverpayment() {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean hasPendingApprovals() {
        return pendingMeasurements != null && pendingMeasurements > 0;
    }
}
