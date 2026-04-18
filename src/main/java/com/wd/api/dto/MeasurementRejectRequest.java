package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;

public record MeasurementRejectRequest(
        @NotBlank(message = "Rejection reason is required")
        String rejectionReason
) {}
