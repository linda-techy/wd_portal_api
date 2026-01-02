package com.wd.api.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when project budget constraints are violated.
 * For example, when budget changes exceed allowed limits.
 */
public class BudgetExceededException extends RuntimeException {

    private final Long projectId;
    private final BigDecimal allocatedBudget;
    private final BigDecimal requestedAmount;

    public BudgetExceededException(Long projectId, BigDecimal allocated, BigDecimal requested) {
        super(String.format("Budget exceeded for project %d: allocated=%s, requested=%s",
                projectId, allocated, requested));
        this.projectId = projectId;
        this.allocatedBudget = allocated;
        this.requestedAmount = requested;
    }

    public BudgetExceededException(String message) {
        super(message);
        this.projectId = null;
        this.allocatedBudget = null;
        this.requestedAmount = null;
    }

    public Long getProjectId() {
        return projectId;
    }

    public BigDecimal getAllocatedBudget() {
        return allocatedBudget;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
