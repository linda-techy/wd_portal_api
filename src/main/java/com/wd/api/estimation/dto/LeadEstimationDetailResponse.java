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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LeadEstimationDetailResponse(
        UUID id,
        String estimationNo,
        Long leadId,
        ProjectType projectType,
        UUID packageId,
        UUID rateVersionId,
        UUID marketIndexId,
        EstimationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal gstAmount,
        BigDecimal grandTotal,
        LocalDate validUntil,
        LocalDateTime createdAt,
        UUID publicViewToken,
        UUID parentEstimationId,
        List<LineItemDto> lineItems,
        List<EstimationSubResourceResponse> inclusions,
        List<EstimationSubResourceResponse> exclusions,
        List<EstimationSubResourceResponse> assumptions,
        List<EstimationSubResourceResponse> paymentMilestones,
        // K — appended for backwards compatibility.
        EstimationPricingMode pricingMode,
        BigDecimal estimatedAreaSqft,
        BigDecimal grandTotalMin,
        BigDecimal grandTotalMax,
        // L — current-estimation indicator. True for the lead's active quote.
        boolean isCurrent,
        // N — raw dimensions input from create-time. Empty for budgetary rows.
        // Exposed so the wizard can hydrate when the user clicks Revise.
        Map<String, Object> dimensionsJson,
        // P — sales-set confidence on budgetary rows; null on line-item rows.
        EstimationConfidenceLevel confidenceLevel,
        // O — discount approval trail. Fields below are null when discount is at or below threshold.
        BigDecimal discountPercent,
        DiscountApprovalStatus discountApprovalStatus,
        Long discountApprovedByUserId,
        LocalDateTime discountApprovedAt,
        String discountApprovalNotes) {

    public static LeadEstimationDetailResponse fromEntity(
            Estimation e,
            List<LineItemDto> lineItems,
            List<EstimationSubResourceResponse> inclusions,
            List<EstimationSubResourceResponse> exclusions,
            List<EstimationSubResourceResponse> assumptions,
            List<EstimationSubResourceResponse> paymentMilestones) {
        return new LeadEstimationDetailResponse(
                e.getId(), e.getEstimationNo(), e.getLeadId(), e.getProjectType(),
                e.getPackageId(), e.getRateVersionId(), e.getMarketIndexId(), e.getStatus(),
                e.getSubtotal(), e.getDiscountAmount(), e.getGstAmount(), e.getGrandTotal(),
                e.getValidUntil(), e.getCreatedAt(), e.getPublicViewToken(),
                e.getParentEstimationId(), lineItems,
                inclusions, exclusions, assumptions, paymentMilestones,
                e.getPricingMode(), e.getEstimatedAreaSqft(),
                e.getGrandTotalMin(), e.getGrandTotalMax(),
                e.isCurrent(),
                e.getDimensionsJson(),
                e.getConfidenceLevel(),
                e.getDiscountPercent(),
                e.getDiscountApprovalStatus(),
                e.getDiscountApprovedByUserId(),
                e.getDiscountApprovedAt(),
                e.getDiscountApprovalNotes());
    }
}
