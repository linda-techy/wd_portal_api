package com.wd.api.dto.scheduling;

import java.time.LocalDate;

/**
 * Row returned by GET /api/tasks/pending-pm-approval. One row per task
 * that's currently in PENDING_PM_APPROVAL across all projects the calling
 * PM has TASK_COMPLETION_APPROVE access to.
 *
 * @param completionPhotoUrl  URL of the most recent geotagged COMPLETION
 *                            site-report photo for the task. Null if the
 *                            site-report exists but has no photos (rare).
 */
public record PendingApprovalRowDto(
        Long taskId,
        String taskTitle,
        Long projectId,
        String projectName,
        LocalDate markedCompleteOn,
        String completionPhotoUrl) { }
