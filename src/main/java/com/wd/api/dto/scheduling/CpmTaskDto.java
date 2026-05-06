package com.wd.api.dto.scheduling;

import java.time.LocalDate;

/**
 * Per-task CPM result row. All date fields may be null on a project that has
 * never had a CPM run yet (first-boot edge case before CpmInitialPopulator).
 */
public record CpmTaskDto(
        Long taskId,
        String taskName,
        Integer durationDays,
        LocalDate esDate,
        LocalDate efDate,
        LocalDate lsDate,
        LocalDate lfDate,
        Integer totalFloatDays,
        boolean isCritical
) {}
