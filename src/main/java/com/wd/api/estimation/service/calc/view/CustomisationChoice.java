package com.wd.api.estimation.service.calc.view;

import com.wd.api.estimation.domain.enums.PricingMode;
import java.math.BigDecimal;
import java.util.UUID;

public record CustomisationChoice(
        UUID categoryId,
        UUID optionId,
        UUID packageDefaultOptionId,
        String description,
        PricingMode pricingMode,
        BigDecimal deltaRate,
        BigDecimal applicableArea) {}
