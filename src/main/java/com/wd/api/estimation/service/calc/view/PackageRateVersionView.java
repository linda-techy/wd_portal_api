package com.wd.api.estimation.service.calc.view;

import java.math.BigDecimal;
import java.util.UUID;

public record PackageRateVersionView(
        UUID id,
        BigDecimal materialRate,
        BigDecimal labourRate,
        BigDecimal overheadRate) {}
