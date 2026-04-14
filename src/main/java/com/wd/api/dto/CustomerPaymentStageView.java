package com.wd.api.dto;

import com.wd.api.model.PaymentStage;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer-facing view of a single payment stage.
 * Deliberately excludes: boqValueSnapshot, retentionHeld, retentionPct,
 * certifiedBy, appliedCreditAmount, netPayableAmount, paidAmount, internal user IDs.
 */
public record CustomerPaymentStageView(
        Integer stageNumber,
        String stageName,
        BigDecimal stageAmountExGst,
        BigDecimal gstAmount,
        BigDecimal stageAmountInclGst,
        BigDecimal stagePercentage,
        String status,
        LocalDate dueDate,
        String milestoneDescription
) {
    public static CustomerPaymentStageView from(PaymentStage stage) {
        return new CustomerPaymentStageView(
                stage.getStageNumber(),
                stage.getStageName(),
                stage.getStageAmountExGst(),
                stage.getGstAmount(),
                stage.getStageAmountInclGst(),
                stage.getStagePercentage(),
                stage.getStatus() != null ? stage.getStatus().name() : null,
                stage.getDueDate(),
                stage.getMilestoneDescription()
        );
    }
}
