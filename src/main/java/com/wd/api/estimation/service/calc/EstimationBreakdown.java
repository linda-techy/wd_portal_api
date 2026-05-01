package com.wd.api.estimation.service.calc;

import java.math.BigDecimal;
import java.util.List;

public record EstimationBreakdown(
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
        List<LineItem> lineItems,
        List<String> warnings) {}
