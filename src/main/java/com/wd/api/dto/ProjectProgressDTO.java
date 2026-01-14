package com.wd.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for project progress information
 * Contains overall progress and breakdown by milestone, task, and budget
 */
public class ProjectProgressDTO {

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("overall_progress")
    private BigDecimal overallProgress;

    @JsonProperty("milestone_progress")
    private BigDecimal milestoneProgress;

    @JsonProperty("task_progress")
    private BigDecimal taskProgress;

    @JsonProperty("budget_progress")
    private BigDecimal budgetProgress;

    @JsonProperty("last_update")
    private LocalDateTime lastUpdate;

    @JsonProperty("calculation_method")
    private String calculationMethod;

    @JsonProperty("milestone_weight")
    private BigDecimal milestoneWeight;

    @JsonProperty("task_weight")
    private BigDecimal taskWeight;

    @JsonProperty("budget_weight")
    private BigDecimal budgetWeight;

    // Additional info
    @JsonProperty("total_milestones")
    private Integer totalMilestones;

    @JsonProperty("completed_milestones")
    private Integer completedMilestones;

    @JsonProperty("total_tasks")
    private Integer totalTasks;

    @JsonProperty("completed_tasks")
    private Integer completedTasks;

    @JsonProperty("total_budget")
    private BigDecimal totalBudget;

    @JsonProperty("spent_amount")
    private BigDecimal spentAmount;

    // Constructors
    public ProjectProgressDTO() {
    }

    public ProjectProgressDTO(Long projectId, BigDecimal overallProgress, BigDecimal milestoneProgress,
                            BigDecimal taskProgress, BigDecimal budgetProgress) {
        this.projectId = projectId;
        this.overallProgress = overallProgress;
        this.milestoneProgress = milestoneProgress;
        this.taskProgress = taskProgress;
        this.budgetProgress = budgetProgress;
    }

    // Getters and Setters
    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(BigDecimal overallProgress) {
        this.overallProgress = overallProgress;
    }

    public BigDecimal getMilestoneProgress() {
        return milestoneProgress;
    }

    public void setMilestoneProgress(BigDecimal milestoneProgress) {
        this.milestoneProgress = milestoneProgress;
    }

    public BigDecimal getTaskProgress() {
        return taskProgress;
    }

    public void setTaskProgress(BigDecimal taskProgress) {
        this.taskProgress = taskProgress;
    }

    public BigDecimal getBudgetProgress() {
        return budgetProgress;
    }

    public void setBudgetProgress(BigDecimal budgetProgress) {
        this.budgetProgress = budgetProgress;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public BigDecimal getMilestoneWeight() {
        return milestoneWeight;
    }

    public void setMilestoneWeight(BigDecimal milestoneWeight) {
        this.milestoneWeight = milestoneWeight;
    }

    public BigDecimal getTaskWeight() {
        return taskWeight;
    }

    public void setTaskWeight(BigDecimal taskWeight) {
        this.taskWeight = taskWeight;
    }

    public BigDecimal getBudgetWeight() {
        return budgetWeight;
    }

    public void setBudgetWeight(BigDecimal budgetWeight) {
        this.budgetWeight = budgetWeight;
    }

    public Integer getTotalMilestones() {
        return totalMilestones;
    }

    public void setTotalMilestones(Integer totalMilestones) {
        this.totalMilestones = totalMilestones;
    }

    public Integer getCompletedMilestones() {
        return completedMilestones;
    }

    public void setCompletedMilestones(Integer completedMilestones) {
        this.completedMilestones = completedMilestones;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }
}

