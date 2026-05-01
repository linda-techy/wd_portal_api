package com.wd.api.estimation.dto.admin;

import com.wd.api.estimation.domain.enums.ProjectType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RateVersionCreateRequest(
        @NotNull UUID packageId,
        @NotNull ProjectType projectType,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal materialRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal labourRate,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999.99") BigDecimal overheadRate,
        // Optional — defaults to today if omitted
        LocalDate effectiveFrom) {}
