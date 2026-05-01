package com.wd.api.estimation.dto;

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
        List<String> warnings) {}
