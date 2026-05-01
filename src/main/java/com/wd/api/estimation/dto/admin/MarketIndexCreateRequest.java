package com.wd.api.estimation.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record MarketIndexCreateRequest(
        // Optional — defaults to today
        LocalDate snapshotDate,
        // Commodity rates: ceiling 99,999.99 to match NUMERIC(10,2) column ceiling (~99,999,999.99
        // technically, but capping at 5 digits before decimal keeps inputs sane and matches the
        // rate-version DTO ceiling for consistency).
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal steelRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal cementRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal sandRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal aggregateRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal tilesRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal electricalRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal paintsRate,
        // Map of commodity_name → weight (BigDecimal serialized as string in JSONB).
        // Sum must be in [0.99, 1.01] — validated server-side.
        @NotNull Map<String, String> weights) {}
