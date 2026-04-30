package com.wd.api.dto.quotation;

import com.wd.api.model.QuotationInclusion;

import java.time.LocalDateTime;

public record InclusionResponse(
        Long id,
        Long quotationId,
        Integer displayOrder,
        String category,
        String text,
        LocalDateTime createdAt
) {
    public static InclusionResponse from(QuotationInclusion entity) {
        return new InclusionResponse(
                entity.getId(),
                entity.getQuotation() != null ? entity.getQuotation().getId() : null,
                entity.getDisplayOrder(),
                entity.getCategory(),
                entity.getText(),
                entity.getCreatedAt()
        );
    }
}
