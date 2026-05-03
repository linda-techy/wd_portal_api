package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.enums.EstimationPricingMode;
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
        // Was @NotNull; now optional because BUDGETARY skips dimensions. Service rejects null when pricingMode = LINE_ITEM.
        @Valid DimensionsDto dimensions,
        @Valid List<CustomisationChoiceDto> customisations,
        @Valid List<SiteFeeRefDto> siteFees,
        @Valid List<AddOnRefDto> addOns,
        @Valid List<GovtFeeRefDto> govtFees,
        @DecimalMin("0.00") @DecimalMax("0.50") BigDecimal discountPercent,
        @DecimalMin("0.00") @DecimalMax("0.50") BigDecimal gstRate,
        EstimationPricingMode pricingMode,
        @DecimalMin("100.00") @DecimalMax("100000.00") BigDecimal estimatedAreaSqft) {

    /**
     * Backwards-compatible 11-arg constructor (pre-K). Defaults pricingMode/estimatedAreaSqft to null,
     * which the service treats as LINE_ITEM mode without an estimated area.
     */
    public CalculatePreviewRequest(
            ProjectType projectType,
            UUID packageId,
            UUID rateVersionIdOverride,
            UUID marketIndexIdOverride,
            DimensionsDto dimensions,
            List<CustomisationChoiceDto> customisations,
            List<SiteFeeRefDto> siteFees,
            List<AddOnRefDto> addOns,
            List<GovtFeeRefDto> govtFees,
            BigDecimal discountPercent,
            BigDecimal gstRate) {
        this(projectType, packageId, rateVersionIdOverride, marketIndexIdOverride,
                dimensions, customisations, siteFees, addOns, govtFees,
                discountPercent, gstRate, null, null);
    }
}
