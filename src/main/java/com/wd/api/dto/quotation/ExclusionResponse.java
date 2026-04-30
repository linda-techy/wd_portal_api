package com.wd.api.dto.quotation;

import com.wd.api.model.QuotationExclusion;

import java.time.LocalDateTime;

public record ExclusionResponse(
        Long id,
        Long quotationId,
        Integer displayOrder,
        String text,
        String costImplicationNote,
        LocalDateTime createdAt
) {
    public static ExclusionResponse from(QuotationExclusion entity) {
        return new ExclusionResponse(
                entity.getId(),
                entity.getQuotation() != null ? entity.getQuotation().getId() : null,
                entity.getDisplayOrder(),
                entity.getText(),
                entity.getCostImplicationNote(),
                entity.getCreatedAt()
        );
    }
}
