package com.wd.api.dto.quotation;

import com.wd.api.model.QuotationPaymentMilestone;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentMilestoneResponse(
        Long id,
        Long quotationId,
        Integer milestoneNumber,
        String triggerEvent,
        BigDecimal percentage,
        BigDecimal amount,
        String notes,
        LocalDateTime createdAt
) {
    public static PaymentMilestoneResponse from(QuotationPaymentMilestone entity) {
        return new PaymentMilestoneResponse(
                entity.getId(),
                entity.getQuotation() != null ? entity.getQuotation().getId() : null,
                entity.getMilestoneNumber(),
                entity.getTriggerEvent(),
                entity.getPercentage(),
                entity.getAmount(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
