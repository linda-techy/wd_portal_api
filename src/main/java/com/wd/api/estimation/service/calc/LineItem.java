package com.wd.api.estimation.service.calc;

import com.wd.api.estimation.domain.enums.LineType;
import java.math.BigDecimal;
import java.util.UUID;

public record LineItem(
        LineType lineType,
        String description,
        UUID sourceRefId,
        BigDecimal quantity,
        String unit,
        BigDecimal unitRate,
        BigDecimal amount,
        int displayOrder) {}
