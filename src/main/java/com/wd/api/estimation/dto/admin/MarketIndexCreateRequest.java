package com.wd.api.estimation.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record MarketIndexCreateRequest(
        // Optional — defaults to today
        LocalDate snapshotDate,
        @NotNull @DecimalMin("0.01") BigDecimal steelRate,
        @NotNull @DecimalMin("0.01") BigDecimal cementRate,
        @NotNull @DecimalMin("0.01") BigDecimal sandRate,
        @NotNull @DecimalMin("0.01") BigDecimal aggregateRate,
        @NotNull @DecimalMin("0.01") BigDecimal tilesRate,
        @NotNull @DecimalMin("0.01") BigDecimal electricalRate,
        @NotNull @DecimalMin("0.01") BigDecimal paintsRate,
        // Map of commodity_name → weight (BigDecimal serialized as string in JSONB).
        // Sum must be in [0.99, 1.01] — validated server-side.
        @NotNull Map<String, String> weights) {}
