package com.wd.api.dto;

import jakarta.validation.constraints.NotNull;

public record ApproveVariationRequest(
    @NotNull(message = "Approver ID is required")
    Long approvedById,

    @NotNull(message = "Approval decision (approve: true/false) is required")
    Boolean approve
) {}
