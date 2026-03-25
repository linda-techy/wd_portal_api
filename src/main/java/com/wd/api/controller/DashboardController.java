package com.wd.api.controller;

import com.wd.api.dto.*;
import com.wd.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Business dashboard aggregation endpoints.
 * Each endpoint is independently cacheable and fetched in parallel by the Flutter app.
 *
 * Authorization: Any authenticated portal user may view aggregate stats.
 * Fine-grained data (individual project/lead) is enforced in the respective controllers.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Top-level KPIs: active projects, open leads, revenue collected, overdue items.
     */
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardOverviewDTO> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    /**
     * Project health: counts by phase/status, budget totals, at-risk projects list.
     */
    @GetMapping("/projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardProjectsDTO> getProjectStats() {
        return ResponseEntity.ok(dashboardService.getProjectStats());
    }

    /**
     * Lead pipeline: by status/source, conversion rate, pipeline value, monthly trend.
     */
    @GetMapping("/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardLeadsDTO> getLeadStats() {
        return ResponseEntity.ok(dashboardService.getLeadStats());
    }

    /**
     * Financial KPIs: revenue collected vs target, cost breakdown, gross margin, monthly trend.
     */
    @GetMapping("/finance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardFinanceDTO> getFinanceStats() {
        return ResponseEntity.ok(dashboardService.getFinanceStats());
    }

    /**
     * Operations pulse: labour on site today, site reports this week, pending approvals, delays.
     */
    @GetMapping("/operations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardOperationsDTO> getOperationsStats() {
        return ResponseEntity.ok(dashboardService.getOperationsStats());
    }
}
