package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFinanceDTO {

    // Revenue side
    private BigDecimal revenueCollected;  // SUM(paidAmount) WHERE status=PAID
    private BigDecimal revenueInvoiced;   // SUM(totalAmount) from ProjectInvoice WHERE ISSUED
    private BigDecimal revenueTarget;     // SUM(totalAmount) from DesignPackagePayment (active)

    // Cost side
    private BigDecimal labourCost;        // SUM(LabourPayment.amount)
    private BigDecimal procurementCost;   // SUM(PurchaseOrder.totalAmount) WHERE not CANCELLED
    private BigDecimal subcontractCost;   // SUM(SubcontractPayment.grossAmount)
    private BigDecimal totalCost;         // labour + procurement + subcontract

    // Profitability
    private BigDecimal grossMargin;       // revenueCollected - totalCost
    private double grossMarginPct;        // grossMargin / revenueCollected * 100

    // Trend data
    private List<MonthlyRevenue> monthlyRevenue; // last 12 months

    // Payment health
    private Map<String, Long> paymentsByStatus; // PENDING/PAID/OVERDUE → count

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;            // yyyy-MM
        private BigDecimal collected;
        private BigDecimal invoiced;
    }
}
