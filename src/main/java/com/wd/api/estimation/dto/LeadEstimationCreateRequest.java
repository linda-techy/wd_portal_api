package com.wd.api.estimation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeadEstimationCreateRequest(
        @NotNull Long leadId,
        @NotNull @Valid CalculatePreviewRequest preview,
        // Optional — defaults to today + 30 days
        LocalDate validUntil) {}
