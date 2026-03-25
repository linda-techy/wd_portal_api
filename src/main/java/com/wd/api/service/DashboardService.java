package com.wd.api.service;

import com.wd.api.dto.*;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ProjectStatus;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerProjectRepository projectRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final LabourPaymentRepository labourPaymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SubcontractPaymentRepository subcontractPaymentRepository;
    private final LabourAttendanceRepository labourAttendanceRepository;
    private final SiteReportRepository siteReportRepository;
    private final ObservationRepository observationRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final DelayLogRepository delayLogRepository;
    private final ProjectInvoiceRepository projectInvoiceRepository;

    // ─── Overview ────────────────────────────────────────────────────────────

    @Cacheable(value = "dashboardOverview", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardOverviewDTO getOverview() {
        long activeProjects = projectRepository.findByProjectStatusAndDeletedAtIsNull(ProjectStatus.ACTIVE).size();
        long totalLeads = leadRepository.count();
        long openLeads = leadRepository.countOpen();
        BigDecimal revenueCollected = paymentScheduleRepository.sumPaidAmount();
        BigDecimal revenueTarget = paymentScheduleRepository.sumTargetAmountActiveProjects();
        BigDecimal pendingPayments = revenueTarget.subtract(revenueCollected).max(BigDecimal.ZERO);
        long overdueProjects = projectRepository.countOverdueProjects();
        long tasksDueToday = taskRepository.countDueToday();
        long overdueTasks = taskRepository.countAllOverdue();

        return DashboardOverviewDTO.builder()
                .totalActiveProjects(activeProjects)
                .totalLeads(totalLeads)
                .openLeads(openLeads)
                .revenueCollected(revenueCollected)
                .revenueTarget(revenueTarget)
                .pendingPayments(pendingPayments)
                .overdueProjects(overdueProjects)
                .tasksDueToday(tasksDueToday)
                .overdueTasks(overdueTasks)
                .build();
    }

    // ─── Projects ────────────────────────────────────────────────────────────

    @Cacheable(value = "dashboardProjects", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardProjectsDTO getProjectStats() {
        // Phase breakdown
        List<Object[]> phaseRows = projectRepository.getProjectCountByPhase();
        Map<String, Long> byPhase = new LinkedHashMap<>();
        for (Object[] row : phaseRows) {
            byPhase.put(row[0].toString(), (Long) row[1]);
        }

        // Status breakdown
        List<Object[]> statusRows = projectRepository.getProjectCountByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long activeCount = 0, completedCount = 0, onHoldCount = 0;
        for (Object[] row : statusRows) {
            String status = row[0].toString();
            long count = (Long) row[1];
            byStatus.put(status, count);
            if ("ACTIVE".equals(status)) activeCount = count;
            else if ("COMPLETED".equals(status)) completedCount = count;
            else if ("ON_HOLD".equals(status)) onHoldCount = count;
        }

        long totalProjects = projectRepository.count();
        long overdueProjects = projectRepository.countOverdueProjects();

        // Budget & sqft aggregation (global totals across all active projects)
        List<CustomerProject> activeProjects = projectRepository.findByProjectStatusAndDeletedAtIsNull(ProjectStatus.ACTIVE);
        BigDecimal totalBudget = activeProjects.stream()
                .map(p -> p.getBudget() != null ? p.getBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSqfeet = activeProjects.stream()
                .map(p -> p.getSqfeet() != null ? p.getSqfeet() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageBudget = activeProjects.isEmpty() ? BigDecimal.ZERO
                : totalBudget.divide(BigDecimal.valueOf(activeProjects.size()), 2, RoundingMode.HALF_UP);

        // At-risk projects: overdue tasks or active delays
        List<DashboardProjectsDTO.ProjectHealthItem> atRisk = buildAtRiskList(activeProjects);

        return DashboardProjectsDTO.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeCount)
                .completedProjects(completedCount)
                .onHoldProjects(onHoldCount)
                .overdueProjects(overdueProjects)
                .byPhase(byPhase)
                .byStatus(byStatus)
                .totalBudget(totalBudget)
                .averageBudget(averageBudget)
                .totalSqfeet(totalSqfeet)
                .atRisk(atRisk)
                .build();
    }

    private List<DashboardProjectsDTO.ProjectHealthItem> buildAtRiskList(List<CustomerProject> projects) {
        LocalDate today = LocalDate.now();
        return projects.stream()
                .map(p -> {
                    int overdueTasks = taskRepository.countOverdueByProjectId(p.getId(), today);
                    int activeDelays = delayLogRepository.countActiveByProjectId(p.getId());
                    return DashboardProjectsDTO.ProjectHealthItem.builder()
                            .projectId(p.getId())
                            .projectName(p.getName())
                            .overdueTasks(overdueTasks)
                            .activeDelays(activeDelays)
                            .budgetUtilizationPct(p.getBudgetProgress())
                            .build();
                })
                .filter(item -> item.getOverdueTasks() > 0 || item.getActiveDelays() > 0)
                .sorted(Comparator.comparingInt(DashboardProjectsDTO.ProjectHealthItem::getOverdueTasks).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    // ─── Leads ───────────────────────────────────────────────────────────────

    @Cacheable(value = "dashboardLeads", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardLeadsDTO getLeadStats() {
        long totalLeads = leadRepository.count();
        long openLeads = leadRepository.countOpen();
        long hotLeads = leadRepository.countHotLeads();

        // Conversion rate
        long converted = leadRepository.countByLeadStatus("converted");
        double conversionRate = totalLeads > 0
                ? Math.round((converted * 100.0 / totalLeads) * 10.0) / 10.0
                : 0.0;

        BigDecimal pipelineValue = leadRepository.pipelineValue();

        // New leads in last 30 days
        long newLeads = leadRepository.countSince(LocalDate.now().minusDays(30));

        // By status map
        List<Object[]> statusRows = leadRepository.countLeadsByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : statusRows) {
            byStatus.put((String) row[0], (Long) row[1]);
        }

        // By source map
        List<Object[]> sourceRows = leadRepository.countLeadsBySource();
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (Object[] row : sourceRows) {
            bySource.put((String) row[0], (Long) row[1]);
        }

        // Monthly trend — last 12 months
        LocalDate fromDate = LocalDate.now().minusMonths(11).with(TemporalAdjusters.firstDayOfMonth());
        List<Object[]> trendRows = leadRepository.monthlyLeadCount(fromDate);
        List<DashboardLeadsDTO.MonthlyCount> monthlyTrend = trendRows.stream()
                .map(row -> DashboardLeadsDTO.MonthlyCount.builder()
                        .month((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return DashboardLeadsDTO.builder()
                .totalLeads(totalLeads)
                .newLeads(newLeads)
                .hotLeads(hotLeads)
                .conversionRate(conversionRate)
                .pipelineValue(pipelineValue)
                .byStatus(byStatus)
                .bySource(bySource)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    // ─── Finance ─────────────────────────────────────────────────────────────

    @Cacheable(value = "dashboardFinance", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardFinanceDTO getFinanceStats() {
        BigDecimal revenueCollected = paymentScheduleRepository.sumPaidAmount();
        BigDecimal revenueInvoiced = projectInvoiceRepository.sumIssuedAmount();
        BigDecimal revenueTarget = paymentScheduleRepository.sumTargetAmountActiveProjects();

        BigDecimal labourCost = labourPaymentRepository.sumTotalLabourCost();
        BigDecimal procurementCost = purchaseOrderRepository.sumTotalProcurementCost();
        BigDecimal subcontractCost = subcontractPaymentRepository.sumTotalSubcontractCost();
        BigDecimal totalCost = labourCost.add(procurementCost).add(subcontractCost);

        BigDecimal grossMargin = revenueCollected.subtract(totalCost);
        double grossMarginPct = revenueCollected.compareTo(BigDecimal.ZERO) > 0
                ? grossMargin.multiply(BigDecimal.valueOf(100))
                        .divide(revenueCollected, 1, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        // Monthly revenue trend (last 12 months)
        LocalDate fromDate = LocalDate.now().minusMonths(11).with(TemporalAdjusters.firstDayOfMonth());
        List<Object[]> collectedRows = paymentScheduleRepository.monthlyRevenueCollected(fromDate);
        List<Object[]> invoicedRows = projectInvoiceRepository.monthlyInvoicedAmount(fromDate);

        Map<String, BigDecimal> collectedMap = new LinkedHashMap<>();
        for (Object[] row : collectedRows) {
            collectedMap.put((String) row[0], row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO);
        }
        Map<String, BigDecimal> invoicedMap = new LinkedHashMap<>();
        for (Object[] row : invoicedRows) {
            invoicedMap.put((String) row[0], row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO);
        }
        // Build unified 12-month list
        List<DashboardFinanceDTO.MonthlyRevenue> monthlyRevenue = buildMonthList(fromDate, collectedMap, invoicedMap);

        // Payment status counts
        List<Object[]> statusRows = paymentScheduleRepository.countByStatus();
        Map<String, Long> paymentsByStatus = new LinkedHashMap<>();
        for (Object[] row : statusRows) {
            paymentsByStatus.put((String) row[0], (Long) row[1]);
        }

        return DashboardFinanceDTO.builder()
                .revenueCollected(revenueCollected)
                .revenueInvoiced(revenueInvoiced)
                .revenueTarget(revenueTarget)
                .labourCost(labourCost)
                .procurementCost(procurementCost)
                .subcontractCost(subcontractCost)
                .totalCost(totalCost)
                .grossMargin(grossMargin)
                .grossMarginPct(grossMarginPct)
                .monthlyRevenue(monthlyRevenue)
                .paymentsByStatus(paymentsByStatus)
                .build();
    }

    private List<DashboardFinanceDTO.MonthlyRevenue> buildMonthList(
            LocalDate from,
            Map<String, BigDecimal> collectedMap,
            Map<String, BigDecimal> invoicedMap) {
        List<DashboardFinanceDTO.MonthlyRevenue> result = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(LocalDate.now())) {
            String key = String.format("%d-%02d", cursor.getYear(), cursor.getMonthValue());
            result.add(DashboardFinanceDTO.MonthlyRevenue.builder()
                    .month(key)
                    .collected(collectedMap.getOrDefault(key, BigDecimal.ZERO))
                    .invoiced(invoicedMap.getOrDefault(key, BigDecimal.ZERO))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    // ─── Operations ──────────────────────────────────────────────────────────

    @Cacheable(value = "dashboardOperations", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardOperationsDTO getOperationsStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();

        long labourOnSite = labourAttendanceRepository.countPresentOnDate(today);
        long siteReports = siteReportRepository.countSince(startOfWeek);
        long overdueTasks = taskRepository.countAllOverdue();
        long tasksDueToday = taskRepository.countDueToday();
        long openObservations = observationRepository.countOpenAll();
        long pendingApprovals = approvalRequestRepository.countPendingAll();
        long activeDelays = delayLogRepository.countActive();

        return DashboardOperationsDTO.builder()
                .labourOnSiteToday(labourOnSite)
                .siteReportsThisWeek(siteReports)
                .totalOverdueTasks(overdueTasks)
                .tasksDueToday(tasksDueToday)
                .openObservations(openObservations)
                .pendingApprovals(pendingApprovals)
                .activeDelays(activeDelays)
                .build();
    }
}
