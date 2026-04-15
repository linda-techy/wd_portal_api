package com.wd.api.dto;

import com.wd.api.model.ChangeOrder;
import com.wd.api.model.ChangeOrderApprovalHistory;
import com.wd.api.model.ChangeOrderPaymentSchedule;
import com.wd.api.model.enums.ApprovalAction;
import com.wd.api.model.enums.VOCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for the Variation Order (VO) lifecycle:
 *   create/update → submit → multi-level approval → payment schedule
 */
public class VariationOrderDtos {

    // =========================================================================
    // Requests
    // =========================================================================

    /** Create or update a draft VO. */
    public record CreateVariationOrderRequest(
            @NotNull Long boqDocumentId,
            @NotBlank @Size(max = 255) String title,
            String description,
            String justification,
            String scopeNotes,
            @NotNull String coType,           // ChangeOrderType enum name
            VOCategory voCategory,
            Long revisesCoId,                 // only for REVISION type
            String mappedStageIds,            // JSON array string, e.g. "[1,2,3]"
            @NotNull @DecimalMin("0.01") BigDecimal netAmountExGst,
            BigDecimal gstRate,
            LocalDate reviewDeadline,
            List<LineItemRequest> lineItems
    ) {}

    public record LineItemRequest(
            Long id,                          // null = new item, non-null = update existing
            @NotBlank String description,
            String unit,
            BigDecimal originalQuantity,
            BigDecimal newQuantity,
            BigDecimal originalRate,
            BigDecimal newRate,
            @NotNull @DecimalMin("0") BigDecimal lineAmountExGst
    ) {}

    /** Submit a draft VO for approval — no body needed beyond the path variable, but kept as record for extensibility. */
    public record SubmitVariationOrderRequest(
            String submissionNote
    ) {}

    /** Approve / reject / escalate / return at one approval level. */
    public record VOApprovalRequest(
            @NotNull ApprovalAction action,
            @NotBlank String comment
    ) {}

    /** Override the VO payment schedule percentages (CUSTOM category or manual adjustment). */
    public record UpdatePaymentScheduleRequest(
            @NotNull Integer advancePct,
            @NotNull Integer progressPct,
            @NotNull Integer completionPct,   // must sum to 100
            Long progressTriggerStageId,
            LocalDate advanceDueDate,
            String completionTrigger
    ) {}

    // =========================================================================
    // Responses
    // =========================================================================

    /** Full VO detail — used on the detail screen. */
    public record VariationOrderResponse(
            Long id,
            Long projectId,
            Long boqDocumentId,
            String referenceNumber,
            String coType,
            String status,
            String title,
            String description,
            String justification,
            String scopeNotes,
            String voCategory,
            Long revisesCoId,
            String mappedStageIds,
            BigDecimal netAmountExGst,
            BigDecimal gstRate,
            BigDecimal gstAmount,
            BigDecimal netAmountInclGst,
            BigDecimal approvedCost,
            boolean advanceCollected,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDateTime rejectedAt,
            String rejectionReason,
            LocalDate reviewDeadline,
            LocalDateTime createdAt,
            List<ApprovalHistoryDto> approvalHistory,
            PaymentScheduleDto paymentSchedule
    ) {
        public static VariationOrderResponse from(
                ChangeOrder co,
                List<ApprovalHistoryDto> history,
                PaymentScheduleDto schedule) {
            return new VariationOrderResponse(
                    co.getId(),
                    co.getProject() != null ? co.getProject().getId() : null,
                    co.getBoqDocument() != null ? co.getBoqDocument().getId() : null,
                    co.getReferenceNumber(),
                    co.getCoType() != null ? co.getCoType().name() : null,
                    co.getStatus() != null ? co.getStatus().name() : null,
                    co.getTitle(),
                    co.getDescription(),
                    co.getJustification(),
                    co.getScopeNotes(),
                    co.getVoCategory() != null ? co.getVoCategory().name() : null,
                    co.getRevisesChangeOrder() != null ? co.getRevisesChangeOrder().getId() : null,
                    co.getMappedStageIds(),
                    co.getNetAmountExGst(),
                    co.getGstRate(),
                    co.getGstAmount(),
                    co.getNetAmountInclGst(),
                    co.getApprovedCost(),
                    co.isAdvanceCollected(),
                    co.getSubmittedAt(),
                    co.getApprovedAt(),
                    co.getRejectedAt(),
                    co.getRejectionReason(),
                    co.getReviewDeadline(),
                    co.getCreatedAt(),
                    history,
                    schedule
            );
        }
    }

    /** Light summary for list views. */
    public record VariationOrderSummary(
            Long id,
            String referenceNumber,
            String title,
            String coType,
            String status,
            String voCategory,
            BigDecimal netAmountInclGst,
            BigDecimal approvedCost,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDateTime createdAt
    ) {
        public static VariationOrderSummary from(ChangeOrder co) {
            return new VariationOrderSummary(
                    co.getId(),
                    co.getReferenceNumber(),
                    co.getTitle(),
                    co.getCoType() != null ? co.getCoType().name() : null,
                    co.getStatus() != null ? co.getStatus().name() : null,
                    co.getVoCategory() != null ? co.getVoCategory().name() : null,
                    co.getNetAmountInclGst(),
                    co.getApprovedCost(),
                    co.getSubmittedAt(),
                    co.getApprovedAt(),
                    co.getCreatedAt()
            );
        }
    }

    /** One row from co_approval_history. */
    public record ApprovalHistoryDto(
            Long id,
            String approverName,
            Long approverId,
            String level,
            String action,
            String comment,
            LocalDateTime actionAt
    ) {
        public static ApprovalHistoryDto from(ChangeOrderApprovalHistory h) {
            return new ApprovalHistoryDto(
                    h.getId(),
                    h.getApproverName(),
                    h.getApprover() != null ? h.getApprover().getId() : null,
                    h.getLevel() != null ? h.getLevel().name() : null,
                    h.getAction() != null ? h.getAction().name() : null,
                    h.getComment(),
                    h.getActionAt()
            );
        }
    }

    /** Payment schedule tranche summary. */
    public record PaymentScheduleDto(
            Long id,
            Integer advancePct,
            BigDecimal advanceAmount,
            String advanceStatus,
            LocalDate advanceDueDate,
            LocalDate advancePaidDate,
            String advanceInvoiceNumber,
            Integer progressPct,
            BigDecimal progressAmount,
            String progressStatus,
            Long progressTriggerStageId,
            LocalDate progressPaidDate,
            Integer completionPct,
            BigDecimal completionAmount,
            String completionStatus,
            String completionTrigger,
            LocalDate completionPaidDate
    ) {
        public static PaymentScheduleDto from(ChangeOrderPaymentSchedule s) {
            return new PaymentScheduleDto(
                    s.getId(),
                    s.getAdvancePct(),
                    s.getAdvanceAmount(),
                    s.getAdvanceStatus() != null ? s.getAdvanceStatus().name() : null,
                    s.getAdvanceDueDate(),
                    s.getAdvancePaidDate(),
                    s.getAdvanceInvoiceNumber(),
                    s.getProgressPct(),
                    s.getProgressAmount(),
                    s.getProgressStatus() != null ? s.getProgressStatus().name() : null,
                    s.getProgressTriggerStage() != null ? s.getProgressTriggerStage().getId() : null,
                    s.getProgressPaidDate(),
                    s.getCompletionPct(),
                    s.getCompletionAmount(),
                    s.getCompletionStatus() != null ? s.getCompletionStatus().name() : null,
                    s.getCompletionTrigger(),
                    s.getCompletionPaidDate()
            );
        }
    }
}
