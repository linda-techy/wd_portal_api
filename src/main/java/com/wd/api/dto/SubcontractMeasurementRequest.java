package com.wd.api.dto;

import com.wd.api.model.SubcontractMeasurement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractMeasurementRequest(
        LocalDate measurementDate,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal rate,
        String billNumber
) {
    public SubcontractMeasurement toEntity() {
        SubcontractMeasurement m = new SubcontractMeasurement();
        m.setMeasurementDate(measurementDate);
        m.setDescription(description);
        m.setQuantity(quantity);
        m.setUnit(unit);
        m.setRate(rate);
        m.setBillNumber(billNumber);
        return m;
    }
}
