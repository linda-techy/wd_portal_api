package com.wd.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record BoqFinancialSummary(
        Long projectId,
        String projectName,
        int totalItems,
        int activeItems,
        BigDecimal totalPlannedCost,
        BigDecimal totalExecutedCost,
        BigDecimal totalBilledCost,
        BigDecimal totalCostToComplete,
        BigDecimal overallExecutionPercentage,
        BigDecimal overallBillingPercentage,
        List<CategoryFinancialBreakdown> categoryBreakdown,
        List<WorkTypeFinancialBreakdown> workTypeBreakdown
) {

    public record CategoryFinancialBreakdown(
            Long categoryId,
            String categoryName,
            int itemCount,
            BigDecimal plannedCost,
            BigDecimal executedCost,
            BigDecimal billedCost,
            BigDecimal costToComplete
    ) {
    }

    public record WorkTypeFinancialBreakdown(
            Long workTypeId,
            String workTypeName,
            int itemCount,
            BigDecimal plannedCost,
            BigDecimal executedCost,
            BigDecimal billedCost,
            BigDecimal costToComplete
    ) {
    }
}
