package com.wd.api.dto.changerequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ChangeRequestTaskPredecessorRequest {
    @NotNull private Long successorCrTaskId;
    @NotNull private Long predecessorCrTaskId;
    @PositiveOrZero private Integer lagDays = 0;

    public Long getSuccessorCrTaskId() { return successorCrTaskId; }
    public void setSuccessorCrTaskId(Long id) { this.successorCrTaskId = id; }
    public Long getPredecessorCrTaskId() { return predecessorCrTaskId; }
    public void setPredecessorCrTaskId(Long id) { this.predecessorCrTaskId = id; }
    public Integer getLagDays() { return lagDays; }
    public void setLagDays(Integer lagDays) { this.lagDays = lagDays == null ? 0 : lagDays; }
}
