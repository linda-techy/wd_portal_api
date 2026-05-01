package com.wd.api.estimation.service.calc.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record MarketIndexSnapshotView(
        UUID id,
        LocalDate snapshotDate,
        BigDecimal compositeIndex,
        Map<String, Object> weights) {}
