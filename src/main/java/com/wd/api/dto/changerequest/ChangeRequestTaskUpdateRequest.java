package com.wd.api.dto.changerequest;

import com.wd.api.model.enums.FloorLoop;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * PATCH body. All fields optional; null means "leave unchanged".
 * Sequence cannot be changed via this endpoint (would require a re-order
 * primitive; YAGNI for v1 — the author can delete + re-add to re-sequence).
 */
public class ChangeRequestTaskUpdateRequest {
    @Size(max = 256)
    private String name;
    @Size(max = 64)
    private String roleHint;
    @Min(1)
    private Integer durationDays;
    private Integer weightFactor;
    private Boolean monsoonSensitive;
    private Boolean isPaymentMilestone;
    private FloorLoop floorLoop;
    private BigDecimal optionalCost;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoleHint() { return roleHint; }
    public void setRoleHint(String roleHint) { this.roleHint = roleHint; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Integer getWeightFactor() { return weightFactor; }
    public void setWeightFactor(Integer weightFactor) { this.weightFactor = weightFactor; }
    public Boolean getMonsoonSensitive() { return monsoonSensitive; }
    public void setMonsoonSensitive(Boolean v) { this.monsoonSensitive = v; }
    public Boolean getIsPaymentMilestone() { return isPaymentMilestone; }
    public void setIsPaymentMilestone(Boolean v) { this.isPaymentMilestone = v; }
    public FloorLoop getFloorLoop() { return floorLoop; }
    public void setFloorLoop(FloorLoop floorLoop) { this.floorLoop = floorLoop; }
    public BigDecimal getOptionalCost() { return optionalCost; }
    public void setOptionalCost(BigDecimal optionalCost) { this.optionalCost = optionalCost; }
}
