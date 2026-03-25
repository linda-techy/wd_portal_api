package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {

    private long totalActiveProjects;
    private long totalLeads;
    private long openLeads;              // status not in [converted, lost]
    private BigDecimal revenueCollected; // SUM(paidAmount) from PaymentSchedule WHERE status=PAID
    private BigDecimal revenueTarget;    // SUM(totalAmount) from DesignPackagePayment (active projects)
    private BigDecimal pendingPayments;  // revenueTarget - revenueCollected
    private long overdueProjects;
    private long tasksDueToday;
    private long overdueTasks;
}
