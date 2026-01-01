package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Accounts Payable Aging DTO
 * Summary of vendor outstanding balances by aging buckets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountsPayableAgingDTO {

    private BigDecimal totalOutstanding;
    private BigDecimal due_0_30_days;
    private BigDecimal due_31_60_days;
    private BigDecimal overdue; // > 60 days

    private Integer totalVendors;
    private Integer totalInvoices;
    private Integer overdueInvoiceCount;

    private List<VendorAgingDetail> vendorBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorAgingDetail {
        private Long vendorId;
        private String vendorName;
        private Integer invoiceCount;
        private BigDecimal totalOutstanding;
        private BigDecimal due_0_30_days;
        private BigDecimal due_31_60_days;
        private BigDecimal overdue;
        private Integer overdueInvoiceCount;
    }

    // Helper methods
    public BigDecimal getCurrentDue() {
        return due_0_30_days != null ? due_0_30_days : BigDecimal.ZERO;
    }

    public boolean hasOverduePayments() {
        return overdue != null && overdue.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getOverduePercentage() {
        if (totalOutstanding == null || totalOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal overdueAmount = overdue != null ? overdue : BigDecimal.ZERO;
        return overdueAmount.multiply(new BigDecimal("100"))
                .divide(totalOutstanding, 2, java.math.RoundingMode.HALF_UP);
    }
}
