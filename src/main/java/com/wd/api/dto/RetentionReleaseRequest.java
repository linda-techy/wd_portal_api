package com.wd.api.dto;

import com.wd.api.model.RetentionRelease;
import com.wd.api.model.SubcontractWorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RetentionReleaseRequest(
        SubcontractWorkOrder workOrder,
        LocalDate releaseDate,
        BigDecimal amountReleased,
        String notes
) {
    public RetentionRelease toEntity() {
        RetentionRelease r = new RetentionRelease();
        r.setWorkOrder(workOrder);
        r.setReleaseDate(releaseDate);
        r.setAmountReleased(amountReleased);
        r.setNotes(notes);
        return r;
    }
}
