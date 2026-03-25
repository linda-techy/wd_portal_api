package com.wd.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateProjectPhaseRequest(
    @NotBlank(message = "Phase name is required")
    String phaseName,

    @NotNull(message = "Planned start date is required")
    LocalDate plannedStart,

    @NotNull(message = "Planned end date is required")
    LocalDate plannedEnd,

    @NotNull(message = "Display order is required")
    @Min(value = 1, message = "Display order must be at least 1")
    Integer displayOrder
) {}
