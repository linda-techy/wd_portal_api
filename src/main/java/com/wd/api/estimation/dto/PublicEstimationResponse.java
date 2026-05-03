package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.enums.ProjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Sanitised estimation view for unauthenticated customer access.
 * Deliberately omits leadId and internal admin fields.
 */
public record PublicEstimationResponse(
        UUID id,
        String estimationNo,
        ProjectType projectType,
        String status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal gstAmount,
        BigDecimal grandTotal,
        LocalDate validUntil,
        LocalDateTime createdAt,
        List<LineItemDto> lineItems,
        List<EstimationSubResourceResponse> inclusions,
        List<EstimationSubResourceResponse> exclusions,
        List<EstimationSubResourceResponse> assumptions,
        List<EstimationSubResourceResponse> paymentMilestones) {
}
