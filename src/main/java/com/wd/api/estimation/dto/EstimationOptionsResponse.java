package com.wd.api.estimation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EstimationOptionsResponse(
        List<CustomisationCategoryDto> customisationCategories,
        List<AddonDto> addons,
        List<SiteFeeDto> siteFees,
        List<GovtFeeDto> govtFees) {

    public record CustomisationCategoryDto(
            UUID id, String name, String pricingMode, int displayOrder,
            List<CustomisationOptionDto> options) {}

    public record CustomisationOptionDto(
            UUID id, UUID categoryId, String name, BigDecimal rate, int displayOrder) {}

    public record AddonDto(
            UUID id, String name, String description, BigDecimal lumpAmount) {}

    public record SiteFeeDto(
            UUID id, String name, String mode, BigDecimal lumpAmount, BigDecimal perSqftRate) {}

    public record GovtFeeDto(
            UUID id, String name, BigDecimal lumpAmount) {}
}
