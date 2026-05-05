package com.wd.api.service.scheduling.dto;

import java.time.LocalDate;

/**
 * One row of the monsoon warning list returned by {@code GET
 * /api/projects/{id}/schedule/warnings}. Severity is one of:
 * <ul>
 *   <li>{@code OVERLAP_FULL} — task lies entirely inside the monsoon window.</li>
 *   <li>{@code OVERLAP_PARTIAL} — task starts before or ends after the window
 *       but spans some part of it.</li>
 * </ul>
 */
public record MonsoonWarning(
        Long taskId,
        String taskName,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        LocalDate monsoonStart,
        LocalDate monsoonEnd,
        String severity
) {}
