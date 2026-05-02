package com.wd.api.estimation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EstimationSubResourceResponse(
        UUID id,
        UUID estimationId,
        String label,
        String description,
        int displayOrder,
        BigDecimal percentage
) {}
