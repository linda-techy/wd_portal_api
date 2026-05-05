package com.wd.api.service.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/projects/{id}/wbs/clone-from-template}.
 * {@code floorCount} is the explicit floor count for PER_FLOOR expansion;
 * if null, the cloner falls back to {@code project.getFloors()}.
 */
public class WbsCloneRequest {

    @NotNull(message = "templateId is required")
    private Long templateId;

    @Min(value = 1, message = "floorCount must be at least 1")
    @Max(value = 20, message = "floorCount must be at most 20")
    private Integer floorCount;

    public WbsCloneRequest() {}

    public WbsCloneRequest(Long templateId, Integer floorCount) {
        this.templateId = templateId;
        this.floorCount = floorCount;
    }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Integer getFloorCount() { return floorCount; }
    public void setFloorCount(Integer floorCount) { this.floorCount = floorCount; }
}
