package com.wd.api.service.scheduling.dto;

import java.time.LocalDate;

public record TaskBaselineDto(
        Long id,
        Long taskId,
        String taskNameAtBaseline,
        LocalDate baselineStart,
        LocalDate baselineEnd,
        Integer baselineDurationDays) {}
