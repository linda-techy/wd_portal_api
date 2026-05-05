package com.wd.api.service.scheduling.dto;

/**
 * DTO carrying a predecessor edge as it appears in WbsTemplateTaskDto.
 * {@code depType} defaults to "FS" when null.
 */
public record WbsTemplateTaskPredecessorDto(
        Long id,
        Long predecessorTaskId,
        Integer lagDays,
        String depType
) {
    public WbsTemplateTaskPredecessorDto {
        if (depType == null) depType = "FS";
        if (lagDays == null) lagDays = 0;
    }
}
