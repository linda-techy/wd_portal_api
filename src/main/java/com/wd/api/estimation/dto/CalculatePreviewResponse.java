package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.enums.EstimationPricingMode;

import java.math.BigDecimal;
import java.util.List;

public record CalculatePreviewResponse(
        BigDecimal chargeableArea,
        BigDecimal baseCost,
        BigDecimal customisationCost,
        BigDecimal siteCost,
        BigDecimal addOnCost,
        BigDecimal fluctuationAdjustment,
        BigDecimal subtotal,
        BigDecimal govtFees,
        BigDecimal discount,
        BigDecimal taxable,
        BigDecimal gst,
        BigDecimal grandTotal,
        List<LineItemDto> lineItems,
        List<String> warnings,
        java.util.UUID rateVersionId,
        java.util.UUID marketIndexId,
        // K — appended for backwards compatibility. Defaults reflect the legacy LINE_ITEM shape.
        EstimationPricingMode pricingMode,
        BigDecimal estimatedAreaSqft,
        BigDecimal grandTotalMin,
        BigDecimal grandTotalMax) {

    /** Pre-K 16-arg constructor — defaults to LINE_ITEM with no range. */
    public CalculatePreviewResponse(
            BigDecimal chargeableArea,
            BigDecimal baseCost,
            BigDecimal customisationCost,
            BigDecimal siteCost,
            BigDecimal addOnCost,
            BigDecimal fluctuationAdjustment,
            BigDecimal subtotal,
            BigDecimal govtFees,
            BigDecimal discount,
            BigDecimal taxable,
            BigDecimal gst,
            BigDecimal grandTotal,
            List<LineItemDto> lineItems,
            List<String> warnings,
            java.util.UUID rateVersionId,
            java.util.UUID marketIndexId) {
        this(chargeableArea, baseCost, customisationCost, siteCost, addOnCost,
                fluctuationAdjustment, subtotal, govtFees, discount, taxable,
                gst, grandTotal, lineItems, warnings, rateVersionId, marketIndexId,
                EstimationPricingMode.LINE_ITEM, null, null, null);
    }
}
