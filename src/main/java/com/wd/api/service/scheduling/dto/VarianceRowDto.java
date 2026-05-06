package com.wd.api.service.scheduling.dto;

import java.time.LocalDate;

/**
 * One row per task in a project's variance report.
 *
 * <p>plannedStart/plannedEnd are the CPM-computed dates from
 * {@code task.es_date} / {@code task.ef_date}. baselineStart/baselineEnd
 * come from the frozen {@code task_baseline} snapshot. Deltas are in
 * working days; positive = slip, null when an actual or baseline is
 * missing.
 */
public record VarianceRowDto(
        Long taskId,
        String taskName,
        LocalDate baselineStart,
        LocalDate baselineEnd,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        LocalDate actualStart,
        LocalDate actualEnd,
        Integer planVsBaselineDays,
        Integer actualVsPlanDays,
        Integer actualVsBaselineDays,
        boolean isCritical) {}
