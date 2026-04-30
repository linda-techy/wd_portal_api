package com.wd.api.dto.quotation;

import com.wd.api.model.QuotationAssumption;

import java.time.LocalDateTime;

public record AssumptionResponse(
        Long id,
        Long quotationId,
        Integer displayOrder,
        String text,
        LocalDateTime createdAt
) {
    public static AssumptionResponse from(QuotationAssumption entity) {
        return new AssumptionResponse(
                entity.getId(),
                entity.getQuotation() != null ? entity.getQuotation().getId() : null,
                entity.getDisplayOrder(),
                entity.getText(),
                entity.getCreatedAt()
        );
    }
}
