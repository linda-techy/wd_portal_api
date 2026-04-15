package com.wd.api.dto;

import com.wd.api.model.FinalAccount;
import com.wd.api.model.enums.FinalAccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs for the Final Account lifecycle:
 *   DRAFT → SUBMITTED → DISPUTED → AGREED → CLOSED
 *
 * Financial totals are stored; computed values (net revised contract value,
 * balance payable) are derived on the response from the entity.
 */
public class FinalAccountDtos {

    // =========================================================================
    // Requests
    // =========================================================================

    /** Create or update a DRAFT final account. All stored totals are editable until SUBMITTED. */
    public record CreateFinalAccountRequest(
            @NotNull BigDecimal baseContractValue,
            BigDecimal totalAdditions,
            BigDecimal totalAcceptedDeductions,
            BigDecimal totalReceivedToDate,
            BigDecimal totalRetentionHeld,
            LocalDate dlpStartDate,
            LocalDate dlpEndDate,
            @NotBlank String preparedBy
    ) {}

    public record UpdateFinalAccountRequest(
            BigDecimal baseContractValue,
            BigDecimal totalAdditions,
            BigDecimal totalAcceptedDeductions,
            BigDecimal totalReceivedToDate,
            BigDecimal totalRetentionHeld,
            LocalDate dlpStartDate,
            LocalDate dlpEndDate,
            String preparedBy
    ) {}

    /** Transition the status — e.g. DRAFT → SUBMITTED, SUBMITTED → AGREED. */
    public record FinalAccountStatusRequest(
            @NotNull FinalAccountStatus targetStatus,
            String agreedBy,          // required when targeting AGREED
            String comment
    ) {}

    /** Release the retention after DLP ends. */
    public record ReleaseRetentionRequest(
            @NotNull LocalDate releaseDate,
            @NotBlank String releasedBy
    ) {}

    // =========================================================================
    // Responses
    // =========================================================================

    public record FinalAccountResponse(
            Long id,
            Long projectId,
            BigDecimal baseContractValue,
            BigDecimal totalAdditions,
            BigDecimal totalAcceptedDeductions,
            /** Computed: base + additions − deductions */
            BigDecimal netRevisedContractValue,
            BigDecimal totalReceivedToDate,
            BigDecimal totalRetentionHeld,
            /** Computed: netRevisedContractValue − totalReceivedToDate */
            BigDecimal balancePayable,
            String status,
            LocalDate dlpStartDate,
            LocalDate dlpEndDate,
            boolean retentionReleased,
            LocalDate retentionReleaseDate,
            String preparedBy,
            String agreedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FinalAccountResponse from(FinalAccount fa) {
            return new FinalAccountResponse(
                    fa.getId(),
                    fa.getProject() != null ? fa.getProject().getId() : null,
                    fa.getBaseContractValue(),
                    fa.getTotalAdditions(),
                    fa.getTotalAcceptedDeductions(),
                    fa.getNetRevisedContractValue(),
                    fa.getTotalReceivedToDate(),
                    fa.getTotalRetentionHeld(),
                    fa.getBalancePayable(),
                    fa.getStatus() != null ? fa.getStatus().name() : null,
                    fa.getDlpStartDate(),
                    fa.getDlpEndDate(),
                    fa.isRetentionReleased(),
                    fa.getRetentionReleaseDate(),
                    fa.getPreparedBy(),
                    fa.getAgreedBy(),
                    fa.getCreatedAt(),
                    fa.getUpdatedAt()
            );
        }
    }
}
