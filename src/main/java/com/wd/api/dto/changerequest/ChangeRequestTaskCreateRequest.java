package com.wd.api.dto.changerequest;

import com.wd.api.model.enums.FloorLoop;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Append-a-proposed-task request body. POSTed to
 * /api/projects/{id}/change-requests/{crId}/tasks. The server allocates the
 * sequence number from {@code countByChangeRequestId} + 1.
 */
public class ChangeRequestTaskCreateRequest {

    @NotBlank
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
    public void setMonsoonSensitive(Boolean monsoonSensitive) { this.monsoonSensitive = monsoonSensitive; }
    public Boolean getIsPaymentMilestone() { return isPaymentMilestone; }
    public void setIsPaymentMilestone(Boolean v) { this.isPaymentMilestone = v; }
    public FloorLoop getFloorLoop() { return floorLoop; }
    public void setFloorLoop(FloorLoop floorLoop) { this.floorLoop = floorLoop; }
    public BigDecimal getOptionalCost() { return optionalCost; }
    public void setOptionalCost(BigDecimal optionalCost) { this.optionalCost = optionalCost; }
}
