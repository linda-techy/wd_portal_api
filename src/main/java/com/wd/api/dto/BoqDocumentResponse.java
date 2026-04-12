package com.wd.api.dto;

import com.wd.api.model.BoqDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BoqDocumentResponse(
        Long id,
        Long projectId,
        String status,
        BigDecimal totalValueExGst,
        BigDecimal gstRate,
        BigDecimal totalGstAmount,
        BigDecimal totalValueInclGst,
        Integer revisionNumber,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime customerApprovedAt,
        LocalDateTime rejectedAt,
        String rejectionReason,
        LocalDateTime createdAt
) {
    public static BoqDocumentResponse from(BoqDocument d) {
        return new BoqDocumentResponse(
                d.getId(),
                d.getProject() != null ? d.getProject().getId() : null,
                d.getStatus() != null ? d.getStatus().name() : null,
                d.getTotalValueExGst(),
                d.getGstRate(),
                d.getTotalGstAmount(),
                d.getTotalValueInclGst(),
                d.getRevisionNumber(),
                d.getSubmittedAt(),
                d.getApprovedAt(),
                d.getCustomerApprovedAt(),
                d.getRejectedAt(),
                d.getRejectionReason(),
                d.getCreatedAt()
        );
    }
}
