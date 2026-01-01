package com.wd.api.controller;

import com.wd.api.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Get Budget vs Actuals summary - compare BOQ planned vs MB actual
     */
    @GetMapping("/summary")
    public ResponseEntity<BudgetService.BudgetVsActualsSummary> getBudgetSummary(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(budgetService.getProjectBudgetSummary(projectId));
    }

    /**
     * Get Project P/L - Revenue minus Expenses
     */
    @GetMapping("/pl")
    public ResponseEntity<BudgetService.ProjectProfitLossSummary> getProjectPL(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(budgetService.getProjectPL(projectId));
    }
}
