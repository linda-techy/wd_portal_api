package com.wd.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TaskProgressUpdateRequest(
        @Min(0) @Max(100) int progressPercent,
        String note
) {}
