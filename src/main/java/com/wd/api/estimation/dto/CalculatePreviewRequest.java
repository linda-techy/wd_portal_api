package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.enums.ProjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CalculatePreviewRequest(
        @NotNull ProjectType projectType,
        @NotNull UUID packageId,
        UUID rateVersionIdOverride,
        UUID marketIndexIdOverride,
        @NotNull @Valid DimensionsDto dimensions,
        @Valid List<CustomisationChoiceDto> customisations,
        @Valid List<SiteFeeRefDto> siteFees,
        @Valid List<AddOnRefDto> addOns,
        @Valid List<GovtFeeRefDto> govtFees,
        // Optional: defaults to 0.00 if omitted (handled in EstimationPreviewService)
        @DecimalMin("0.00") @DecimalMax("0.50") BigDecimal discountPercent,
        // Optional: defaults to 0.18 if omitted (handled in EstimationPreviewService)
        @DecimalMin("0.00") @DecimalMax("0.50") BigDecimal gstRate) {}
