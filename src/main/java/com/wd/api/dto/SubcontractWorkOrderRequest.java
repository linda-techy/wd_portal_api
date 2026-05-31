package com.wd.api.dto;

import com.wd.api.model.BoqItem;
import com.wd.api.model.SubcontractWorkOrder;
import com.wd.api.model.SubcontractWorkOrder.MeasurementBasis;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractWorkOrderRequest(
        String workOrderNumber,
        BoqItem boqItem,
        String scopeDescription,
        MeasurementBasis measurementBasis,
        BigDecimal negotiatedAmount,
        String unit,
        BigDecimal rate,
        LocalDate startDate,
        LocalDate targetCompletionDate,
        LocalDate actualCompletionDate,
        String paymentTerms,
        BigDecimal retentionPercentage
) {
    public SubcontractWorkOrder toEntity() {
        SubcontractWorkOrder w = new SubcontractWorkOrder();
        w.setWorkOrderNumber(workOrderNumber);
        w.setBoqItem(boqItem);
        w.setScopeDescription(scopeDescription);
        w.setMeasurementBasis(measurementBasis);
        w.setNegotiatedAmount(negotiatedAmount);
        w.setUnit(unit);
        w.setRate(rate);
        w.setStartDate(startDate);
        w.setTargetCompletionDate(targetCompletionDate);
        w.setActualCompletionDate(actualCompletionDate);
        w.setPaymentTerms(paymentTerms);
        w.setRetentionPercentage(retentionPercentage);
        return w;
    }
}
