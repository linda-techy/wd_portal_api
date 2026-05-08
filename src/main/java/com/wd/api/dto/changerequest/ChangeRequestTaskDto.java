package com.wd.api.dto.changerequest;

import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.enums.FloorLoop;

import java.math.BigDecimal;

/** Read response for a single CR proposed task. */
public class ChangeRequestTaskDto {
    private Long id;
    private Long changeRequestId;
    private Integer sequence;
    private String name;
    private String roleHint;
    private Integer durationDays;
    private Integer weightFactor;
    private Boolean monsoonSensitive;
    private Boolean isPaymentMilestone;
    private FloorLoop floorLoop;
    private BigDecimal optionalCost;

    public static ChangeRequestTaskDto from(ChangeRequestTask t) {
        ChangeRequestTaskDto d = new ChangeRequestTaskDto();
        d.id = t.getId();
        d.changeRequestId = t.getChangeRequest() != null ? t.getChangeRequest().getId() : null;
        d.sequence = t.getSequence();
        d.name = t.getName();
        d.roleHint = t.getRoleHint();
        d.durationDays = t.getDurationDays();
        d.weightFactor = t.getWeightFactor();
        d.monsoonSensitive = t.getMonsoonSensitive();
        d.isPaymentMilestone = t.getIsPaymentMilestone();
        d.floorLoop = t.getFloorLoop();
        d.optionalCost = t.getOptionalCost();
        return d;
    }

    public Long getId() { return id; }
    public Long getChangeRequestId() { return changeRequestId; }
    public Integer getSequence() { return sequence; }
    public String getName() { return name; }
    public String getRoleHint() { return roleHint; }
    public Integer getDurationDays() { return durationDays; }
    public Integer getWeightFactor() { return weightFactor; }
    public Boolean getMonsoonSensitive() { return monsoonSensitive; }
    public Boolean getIsPaymentMilestone() { return isPaymentMilestone; }
    public FloorLoop getFloorLoop() { return floorLoop; }
    public BigDecimal getOptionalCost() { return optionalCost; }
}
