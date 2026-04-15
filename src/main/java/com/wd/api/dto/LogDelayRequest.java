package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LogDelayRequest(
    Long phaseId,

    @NotBlank(message = "Delay type is required")
    String delayType,

    @NotNull(message = "From date is required")
    LocalDate fromDate,

    LocalDate toDate,

    @NotBlank(message = "Reason is required")
    String reason,

    String impactDescription,

    Long loggedById
) {}
