package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.enums.DiscountApprovalStatus;
import com.wd.api.estimation.domain.enums.EstimationConfidenceLevel;
import com.wd.api.estimation.domain.enums.EstimationPricingMode;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.ProjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeadEstimationSummaryResponse(
        UUID id,
        String estimationNo,
        Long leadId,
        ProjectType projectType,
        UUID packageId,
        EstimationStatus status,
        BigDecimal grandTotal,
        LocalDate validUntil,
        LocalDateTime createdAt,
        UUID publicViewToken,
        UUID parentEstimationId,
        // K — appended for backwards compatibility.
        EstimationPricingMode pricingMode,
        BigDecimal estimatedAreaSqft,
        BigDecimal grandTotalMin,
        BigDecimal grandTotalMax,
        // L — current-estimation indicator.
        boolean isCurrent,
        // P — sales-set confidence (only on budgetary rows).
        EstimationConfidenceLevel confidenceLevel,
        // O — discount approval status (null when no approval needed).
        DiscountApprovalStatus discountApprovalStatus) {

    public static LeadEstimationSummaryResponse fromEntity(Estimation e) {
        return new LeadEstimationSummaryResponse(
                e.getId(),
                e.getEstimationNo(),
                e.getLeadId(),
                e.getProjectType(),
                e.getPackageId(),
                e.getStatus(),
                e.getGrandTotal(),
                e.getValidUntil(),
                e.getCreatedAt(),
                e.getPublicViewToken(),
                e.getParentEstimationId(),
                e.getPricingMode(),
                e.getEstimatedAreaSqft(),
                e.getGrandTotalMin(),
                e.getGrandTotalMax(),
                e.isCurrent(),
                e.getConfidenceLevel(),
                e.getDiscountApprovalStatus());
    }
}
