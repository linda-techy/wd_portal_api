package com.wd.api.dto;

import com.wd.api.model.DeductionRegister;
import com.wd.api.model.enums.DeductionDecision;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs for the Deduction Register lifecycle:
 *   create → decision (accept/partially/reject) → escalate → settle in final account
 */
public class DeductionRegisterDtos {

    // =========================================================================
    // Requests
    // =========================================================================

    public record CreateDeductionRequest(
            Long coId,                        // optional — links to the OMISSION CO
            @NotBlank String itemDescription,
            @NotNull @DecimalMin("0.01") BigDecimal requestedAmount
    ) {}

    public record DeductionDecisionRequest(
            @NotNull DeductionDecision decision,
            BigDecimal acceptedAmount,        // required for PARTIALLY_ACCEPTABLE
            String rejectionReason,           // required for REJECTED
            @NotBlank String approvedBy,
            LocalDate decisionDate
    ) {}

    public record EscalateDeductionRequest(
            @NotBlank String escalatedTo,
            String comment
    ) {}

    // =========================================================================
    // Responses
    // =========================================================================

    public record DeductionRegisterResponse(
            Long id,
            Long projectId,
            Long coId,
            String itemDescription,
            BigDecimal requestedAmount,
            BigDecimal acceptedAmount,
            String decision,
            String rejectionReason,
            String escalationStatus,
            String escalatedTo,
            boolean settledInFinalAccount,
            String approvedBy,
            LocalDate decisionDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DeductionRegisterResponse from(DeductionRegister d) {
            return new DeductionRegisterResponse(
                    d.getId(),
                    d.getProject() != null ? d.getProject().getId() : null,
                    d.getChangeOrder() != null ? d.getChangeOrder().getId() : null,
                    d.getItemDescription(),
                    d.getRequestedAmount(),
                    d.getAcceptedAmount(),
                    d.getDecision() != null ? d.getDecision().name() : null,
                    d.getRejectionReason(),
                    d.getEscalationStatus() != null ? d.getEscalationStatus().name() : null,
                    d.getEscalatedTo(),
                    d.isSettledInFinalAccount(),
                    d.getApprovedBy(),
                    d.getDecisionDate(),
                    d.getCreatedAt(),
                    d.getUpdatedAt()
            );
        }
    }
}
