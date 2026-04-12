package com.wd.api.dto;

import com.wd.api.model.PaymentStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentStageResponse(
        Long id,
        Long boqDocumentId,
        Long projectId,
        Integer stageNumber,
        String stageName,
        BigDecimal boqValueSnapshot,
        BigDecimal stagePercentage,
        BigDecimal stageAmountExGst,
        BigDecimal gstRate,
        BigDecimal gstAmount,
        BigDecimal stageAmountInclGst,
        BigDecimal appliedCreditAmount,
        BigDecimal netPayableAmount,
        BigDecimal paidAmount,
        String status,
        LocalDate dueDate,
        String milestoneDescription,
        Long invoiceId,
        LocalDateTime paidAt
) {
    public static PaymentStageResponse from(PaymentStage s) {
        return new PaymentStageResponse(
                s.getId(),
                s.getBoqDocument() != null ? s.getBoqDocument().getId() : null,
                s.getProject() != null ? s.getProject().getId() : null,
                s.getStageNumber(),
                s.getStageName(),
                s.getBoqValueSnapshot(),
                s.getStagePercentage(),
                s.getStageAmountExGst(),
                s.getGstRate(),
                s.getGstAmount(),
                s.getStageAmountInclGst(),
                s.getAppliedCreditAmount(),
                s.getNetPayableAmount(),
                s.getPaidAmount(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getDueDate(),
                s.getMilestoneDescription(),
                s.getInvoice() != null ? s.getInvoice().getId() : null,
                s.getPaidAt()
        );
    }
}
