package com.wd.api.dto;

import com.wd.api.model.CustomerProject;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProjectSummaryDTO {
    // Core Details
    private CustomerProject project;

    // Team
    private List<ProjectMemberDTO> teamMembers;

    // Execution Status
    private ProjectExecutionStats executionStats;

    // Financial Snapshot
    private FinancialSnapshot financialSnapshot;

    // Recent Activity
    private List<ActivityFeedDTO> recentActivities;

    @Data
    @Builder
    public static class ProjectMemberDTO {
        private Long userId;
        private String name;
        private String role;
        private String email;
    }

    @Data
    @Builder
    public static class ProjectExecutionStats {
        private int totalTasks;
        private int completedTasks;
        private int overdueTasks;
        private int activeDelays; // Count of open delay logs
    }

    @Data
    @Builder
    public static class FinancialSnapshot {
        private BigDecimal totalBudget; // From Leads/BoQ
        private BigDecimal totalInvoiced;
        private BigDecimal totalPaid;
        private BigDecimal pendingPayments;
    }
}
