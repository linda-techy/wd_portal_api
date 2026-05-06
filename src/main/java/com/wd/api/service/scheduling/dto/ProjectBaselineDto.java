package com.wd.api.service.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectBaselineDto(
        Long id,
        Long projectId,
        LocalDateTime approvedAt,
        Long approvedBy,
        LocalDate projectStartDate,
        LocalDate projectFinishDate,
        List<TaskBaselineDto> tasks) {}
