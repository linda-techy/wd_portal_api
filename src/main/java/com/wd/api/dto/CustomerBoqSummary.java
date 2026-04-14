package com.wd.api.dto;

import com.wd.api.model.BoqDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer-facing summary of a BOQ document.
 * Deliberately excludes: unit rates, execution quantities, billing data,
 * cost-to-complete, internal notes, and internal user IDs.
 */
public record CustomerBoqSummary(
        Long documentId,
        Long projectId,
        String projectName,
        BigDecimal totalValueExGst,
        BigDecimal totalGstAmount,
        BigDecimal totalValueInclGst,
        BigDecimal gstRate,
        String status,
        Integer revisionNumber,
        LocalDateTime approvedAt,
        LocalDateTime acknowledgedAt,
        boolean pendingAcknowledgement,
        List<CustomerPaymentStageView> paymentStages
) {
    public static CustomerBoqSummary from(BoqDocument doc, List<CustomerPaymentStageView> stages) {
        boolean pending = doc.isApproved() && doc.getCustomerAcknowledgedAt() == null;
        return new CustomerBoqSummary(
                doc.getId(),
                doc.getProject().getId(),
                doc.getProject().getName(),
                doc.getTotalValueExGst(),
                doc.getTotalGstAmount(),
                doc.getTotalValueInclGst(),
                doc.getGstRate(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getRevisionNumber(),
                doc.getCustomerApprovedAt(),
                doc.getCustomerAcknowledgedAt(),
                pending,
                stages
        );
    }
}
