package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vendor Outstanding DTO
 * Summary of outstanding balance for a specific vendor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorOutstandingDTO {

    private Long vendorId;
    private String vendorName;
    private String vendorGstin;
    private String contactPerson;
    private String phone;

    private Integer totalInvoices;
    private BigDecimal totalInvoiced;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;

    private Integer overdueInvoiceCount;
    private BigDecimal overdueAmount;
    private LocalDate oldestDueDate;

    private Integer unpaidInvoiceCount;
    private Integer partiallyPaidInvoiceCount;

    // Helper methods
    public boolean hasOverduePayments() {
        return overdueAmount != null && overdueAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getPaymentHealthStatus() {
        if (overdueAmount != null && overdueAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "OVERDUE";
        }
        if (unpaidInvoiceCount != null && unpaidInvoiceCount > 0) {
            return "DUE";
        }
        if (partiallyPaidInvoiceCount != null && partiallyPaidInvoiceCount > 0) {
            return "PARTIAL";
        }
        return "CLEAR";
    }

    public BigDecimal getPaymentPercentage() {
        if (totalInvoiced == null || totalInvoiced.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal paid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
        return paid.multiply(new BigDecimal("100"))
                .divide(totalInvoiced, 2, java.math.RoundingMode.HALF_UP);
    }
}
