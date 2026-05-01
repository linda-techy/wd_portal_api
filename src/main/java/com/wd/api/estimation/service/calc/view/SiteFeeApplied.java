package com.wd.api.estimation.service.calc.view;

import com.wd.api.estimation.domain.enums.SiteFeeMode;
import java.math.BigDecimal;
import java.util.UUID;

public record SiteFeeApplied(
        UUID id,
        String name,
        SiteFeeMode mode,
        BigDecimal lumpAmount,
        BigDecimal perSqftRate) {}
