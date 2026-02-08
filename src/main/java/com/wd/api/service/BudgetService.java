package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

        private final BoqItemRepository boqItemRepository;
        private final MeasurementBookRepository measurementBookRepository;
        private final PurchaseOrderRepository purchaseOrderRepository;
        private final PaymentTransactionRepository paymentTransactionRepository;
        private final PaymentScheduleRepository paymentScheduleRepository;

        /**
         * Get Budget vs Actuals summary for a project
         */
        public BudgetVsActualsSummary getProjectBudgetSummary(Long projectId) {
                List<BoqItem> boqItems = boqItemRepository.findByProjectId(projectId);
                List<MeasurementBook> mbEntries = measurementBookRepository.findByProjectId(projectId);

                // Group MB entries by BOQ Item
                Map<Long, List<MeasurementBook>> mbByBoq = mbEntries.stream()
                                .filter(mb -> mb.getBoqItem() != null)
                                .collect(Collectors.groupingBy(mb -> mb.getBoqItem().getId()));

                List<BoqItemActual> itemActuals = new ArrayList<>();
                BigDecimal totalBudget = BigDecimal.ZERO;
                BigDecimal totalActual = BigDecimal.ZERO;

                for (BoqItem boq : boqItems) {
                        BigDecimal budgetAmount = boq.getTotalAmount() != null ? boq.getTotalAmount() : BigDecimal.ZERO;
                        BigDecimal budgetQty = boq.getQuantity() != null ? boq.getQuantity() : BigDecimal.ZERO;

                        List<MeasurementBook> mbList = mbByBoq.getOrDefault(boq.getId(), Collections.emptyList());
                        BigDecimal actualQty = mbList.stream()
                                        .map(MeasurementBook::getQuantity)
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal actualAmount = mbList.stream()
                                        .map(MeasurementBook::getTotalAmount)
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal variance = budgetAmount.subtract(actualAmount);
                        double consumedPercentage = budgetQty.compareTo(BigDecimal.ZERO) > 0
                                        ? actualQty.divide(budgetQty, 4, RoundingMode.HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100))
                                                        .doubleValue()
                                        : 0.0;

                        String status = calculateItemStatus(budgetAmount, actualAmount);

                        itemActuals.add(new BoqItemActual(
                                        boq.getId(),
                                        boq.getDescription(),
                                        boq.getWorkType() != null ? boq.getWorkType().getName() : "N/A",
                                        boq.getUnit(),
                                        budgetQty,
                                        budgetAmount,
                                        actualQty,
                                        actualAmount,
                                        variance,
                                        consumedPercentage,
                                        status));

                        totalBudget = totalBudget.add(budgetAmount);
                        totalActual = totalActual.add(actualAmount);
                }

                BigDecimal totalVariance = totalBudget.subtract(totalActual);
                double overallConsumed = totalBudget.compareTo(BigDecimal.ZERO) > 0
                                ? totalActual.divide(totalBudget, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .doubleValue()
                                : 0.0;

                String overallStatus = calculateOverallBudgetStatus(overallConsumed);

                return new BudgetVsActualsSummary(
                                projectId,
                                totalBudget,
                                totalActual,
                                totalVariance,
                                overallConsumed,
                                overallStatus,
                                itemActuals);
        }

        /**
         * Get Project P/L Summary
         */
        public ProjectProfitLossSummary getProjectPL(Long projectId) {
                // Revenue: Sum of payment transactions for project's schedules
                BigDecimal totalRevenue = BigDecimal.ZERO;
                List<PaymentSchedule> schedules = paymentScheduleRepository.findByDesignPayment_Project_Id(projectId);
                for (PaymentSchedule schedule : schedules) {
                        List<PaymentTransaction> transactions = paymentTransactionRepository
                                        .findByScheduleId(schedule.getId());
                        for (PaymentTransaction t : transactions) {
                                if ("COMPLETED".equals(t.getStatus()) && t.getNetAmount() != null) {
                                        totalRevenue = totalRevenue.add(t.getNetAmount());
                                }
                        }
                }

                // Material Cost: Sum of PO amounts for project
                List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByProjectId(projectId);
                BigDecimal materialCost = purchaseOrders.stream()
                                .filter(po -> po.getStatus() != com.wd.api.model.enums.PurchaseOrderStatus.CANCELLED)
                                .map(po -> po.getNetAmount() != null ? po.getNetAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Labour Cost: From Measurement Books (labour-related entries)
                List<MeasurementBook> mbEntries = measurementBookRepository.findByProjectId(projectId);
                BigDecimal labourCost = mbEntries.stream()
                                .filter(mb -> mb.getLabour() != null)
                                .map(mb -> mb.getTotalAmount() != null ? mb.getTotalAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpenses = materialCost.add(labourCost);
                BigDecimal grossProfit = totalRevenue.subtract(totalExpenses);
                double profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .doubleValue()
                                : 0.0;

                String plStatus = grossProfit.compareTo(BigDecimal.ZERO) >= 0 ? "PROFIT" : "LOSS";

                return new ProjectProfitLossSummary(
                                projectId,
                                totalRevenue,
                                materialCost,
                                labourCost,
                                totalExpenses,
                                grossProfit,
                                profitMargin,
                                plStatus);
        }

        private String calculateItemStatus(BigDecimal budget, BigDecimal actual) {
                if (budget.compareTo(BigDecimal.ZERO) == 0)
                        return "NO_BUDGET";
                double ratio = actual.divide(budget, 4, RoundingMode.HALF_UP).doubleValue();
                if (ratio >= 1.0)
                        return "OVER_BUDGET";
                if (ratio >= 0.8)
                        return "NEAR_LIMIT";
                return "ON_TRACK";
        }

        private String calculateOverallBudgetStatus(double consumedPercentage) {
                if (consumedPercentage >= 100)
                        return "OVER_BUDGET";
                if (consumedPercentage >= 80)
                        return "NEAR_LIMIT";
                return "ON_TRACK";
        }

        // DTOs
        public record BoqItemActual(
                        Long boqItemId,
                        String description,
                        String workType,
                        String unit,
                        BigDecimal budgetQty,
                        BigDecimal budgetAmount,
                        BigDecimal actualQty,
                        BigDecimal actualAmount,
                        BigDecimal variance,
                        double consumedPercentage,
                        String status) {
        }

        public record BudgetVsActualsSummary(
                        Long projectId,
                        BigDecimal totalBudget,
                        BigDecimal totalActual,
                        BigDecimal variance,
                        double consumedPercentage,
                        String status,
                        List<BoqItemActual> items) {
        }

        public record ProjectProfitLossSummary(
                        Long projectId,
                        BigDecimal totalRevenue,
                        BigDecimal materialCost,
                        BigDecimal labourCost,
                        BigDecimal totalExpenses,
                        BigDecimal grossProfit,
                        double profitMarginPercentage,
                        String status) {
        }
}
