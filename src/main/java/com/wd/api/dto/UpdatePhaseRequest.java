package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdatePhaseRequest(
    @NotBlank(message = "Status is required")
    String status,

    LocalDate actualStart,

    LocalDate actualEnd
) {}
