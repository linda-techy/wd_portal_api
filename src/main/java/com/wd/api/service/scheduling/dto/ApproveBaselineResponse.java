package com.wd.api.service.scheduling.dto;

import java.time.LocalDate;

public record ApproveBaselineResponse(
        Long baselineId,
        Long projectId,
        LocalDate projectStartDate,
        LocalDate projectFinishDate,
        int taskCount) {}
