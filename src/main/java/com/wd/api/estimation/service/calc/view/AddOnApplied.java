package com.wd.api.estimation.service.calc.view;

import java.math.BigDecimal;
import java.util.UUID;

public record AddOnApplied(UUID id, String name, BigDecimal lumpAmount) {}
