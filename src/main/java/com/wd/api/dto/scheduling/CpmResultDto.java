package com.wd.api.dto.scheduling;

import java.time.LocalDate;
import java.util.List;

/**
 * Project-level CPM snapshot served by GET /api/projects/{id}/cpm.
 *
 * @param projectId          which project
 * @param projectStartDate   resolved start (CustomerProject.startDate -> first task's startDate -> today)
 * @param projectFinishDate  max(efDate) across all tasks; null if no tasks
 * @param criticalTaskIds    ids of tasks where {@code isCritical=true}
 * @param tasks              every task with its CPM-derived dates
 */
public record CpmResultDto(
        Long projectId,
        LocalDate projectStartDate,
        LocalDate projectFinishDate,
        List<Long> criticalTaskIds,
        List<CpmTaskDto> tasks
) {}
