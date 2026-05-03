package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.ProjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        List<EstimationSubResourceResponse> paymentMilestones) {

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
                inclusions, exclusions, assumptions, paymentMilestones);
    }
}
