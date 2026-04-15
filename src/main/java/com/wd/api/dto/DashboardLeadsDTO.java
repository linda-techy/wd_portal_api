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
public class DashboardLeadsDTO {

    private long totalLeads;
    private long newLeads;           // created in last 30 days
    private long hotLeads;           // scoreCategory = 'HOT'
    private double conversionRate;   // (converted / total) * 100
    private BigDecimal pipelineValue; // SUM(budget) WHERE status IN [qualified, proposal_sent]
    private Map<String, Long> byStatus;  // status → count
    private Map<String, Long> bySource;  // source → count
    private List<MonthlyCount> monthlyTrend; // last 12 months new leads

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCount {
        private String month; // yyyy-MM
        private long count;
    }
}
