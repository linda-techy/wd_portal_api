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
public class DashboardProjectsDTO {

    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long onHoldProjects;
    private long overdueProjects;
    private Map<String, Long> byPhase;        // phase name → count
    private Map<String, Long> byStatus;       // status name → count
    private BigDecimal totalBudget;
    private BigDecimal averageBudget;
    private BigDecimal totalSqfeet;
    private List<ProjectHealthItem> atRisk;   // top 5 projects needing attention

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectHealthItem {
        private Long projectId;
        private String projectName;
        private int overdueTasks;
        private int activeDelays;
        private BigDecimal budgetUtilizationPct; // spent/budget * 100 (if available)
    }
}
