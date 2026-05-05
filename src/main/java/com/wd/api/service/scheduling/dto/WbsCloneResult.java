package com.wd.api.service.scheduling.dto;

/**
 * Summary returned by WbsTemplateClonerService.cloneInto.
 */
public record WbsCloneResult(
        int milestonesCreated,
        int tasksCreated,
        int predecessorsCreated
) {}
