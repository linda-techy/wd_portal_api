package com.wd.api.dto.changerequest;

/**
 * Returned by ChangeRequestMergeService.mergeIntoWbs and surfaced from the
 * APPROVED -> SCHEDULED endpoint for caller diagnostics.
 */
public record ChangeRequestMergeResult(
        int tasksCreated,
        int proposedTasksCount,
        int predecessorEdgesCreated,
        boolean handoverCheckRan) {}
