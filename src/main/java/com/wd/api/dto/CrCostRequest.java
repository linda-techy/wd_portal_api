package com.wd.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CrCostRequest {
    @NotNull
    private BigDecimal costImpact;       // signed BigDecimal: positive = added cost, negative = saving
    @NotNull
    @PositiveOrZero
    private Integer timeImpactWorkingDays;

    public BigDecimal getCostImpact() { return costImpact; }
    public void setCostImpact(BigDecimal costImpact) { this.costImpact = costImpact; }
    public Integer getTimeImpactWorkingDays() { return timeImpactWorkingDays; }
    public void setTimeImpactWorkingDays(Integer t) { this.timeImpactWorkingDays = t; }
}
