package com.wd.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CrScheduleRequest {
    @NotNull
    @Positive
    private Long anchorTaskId;

    public Long getAnchorTaskId() { return anchorTaskId; }
    public void setAnchorTaskId(Long anchorTaskId) { this.anchorTaskId = anchorTaskId; }
}
