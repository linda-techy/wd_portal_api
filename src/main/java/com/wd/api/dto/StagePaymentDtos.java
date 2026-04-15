package com.wd.api.dto;

import com.wd.api.model.PaymentStage;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs for Stage Payment certification and retention management.
 *
 * Lifecycle: UPCOMING → DUE → INVOICED → PAID
 *
 * Certification records who signed off and computes retention held.
 */
public class StagePaymentDtos {

    // =========================================================================
    // Requests
    // =========================================================================

    /** Certify a payment stage — records the certifier and retention rate. */
    public record CertifyStageRequest(
            @NotNull String certifiedBy,
            /** Retention percentage to hold. Defaults to 5% if null. */
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal retentionPct
    ) {}

    /** Mark a certified stage as invoiced. */
    public record InvoiceStageRequest(
            @NotNull Long invoiceId
    ) {}

    /** Record a payment against a stage. */
    public record RecordStagePaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal paidAmount,
            LocalDate paidDate
    ) {}

    // =========================================================================
    // Responses
    // =========================================================================

    /** Full stage detail including certification and retention fields. */
    public record StageCertificationResponse(
            Long id,
            Long projectId,
            Long boqDocumentId,
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
            LocalDateTime paidAt,
            // certification fields
            String certifiedBy,
            BigDecimal retentionPct,
            BigDecimal retentionHeld,
            LocalDateTime certifiedAt
    ) {
        public static StageCertificationResponse from(PaymentStage s) {
            return new StageCertificationResponse(
                    s.getId(),
                    s.getProject() != null ? s.getProject().getId() : null,
                    s.getBoqDocument() != null ? s.getBoqDocument().getId() : null,
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
                    s.getPaidAt(),
                    s.getCertifiedBy(),
                    s.getRetentionPct(),
                    s.getRetentionHeld(),
                    s.getCertifiedAt()
            );
        }
    }

    /** Summary for the stage timeline list view. */
    public record StageTimelineSummary(
            Long id,
            Integer stageNumber,
            String stageName,
            BigDecimal stageAmountInclGst,
            BigDecimal netPayableAmount,
            BigDecimal retentionHeld,
            String status,
            LocalDate dueDate,
            boolean certified,
            LocalDateTime certifiedAt
    ) {
        public static StageTimelineSummary from(PaymentStage s) {
            return new StageTimelineSummary(
                    s.getId(),
                    s.getStageNumber(),
                    s.getStageName(),
                    s.getStageAmountInclGst(),
                    s.getNetPayableAmount(),
                    s.getRetentionHeld(),
                    s.getStatus() != null ? s.getStatus().name() : null,
                    s.getDueDate(),
                    s.getCertifiedAt() != null,
                    s.getCertifiedAt()
            );
        }
    }
}
