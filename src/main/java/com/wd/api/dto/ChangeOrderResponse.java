package com.wd.api.dto;

import com.wd.api.model.ChangeOrder;
import com.wd.api.model.ChangeOrderLineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ChangeOrderResponse(
        Long id,
        Long projectId,
        Long boqDocumentId,
        String referenceNumber,
        String coType,
        String status,
        String title,
        String description,
        String justification,
        BigDecimal netAmountExGst,
        BigDecimal gstRate,
        BigDecimal gstAmount,
        BigDecimal netAmountInclGst,
        LocalDateTime submittedAt,
        LocalDateTime internallyApprovedAt,
        Long internallyApprovedBy,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason,
        LocalDate reviewDeadline,
        LocalDateTime createdAt,
        List<LineItemDto> lineItems
) {

    public record LineItemDto(
            Long id,
            String description,
            String unit,
            BigDecimal originalQuantity,
            BigDecimal newQuantity,
            BigDecimal deltaQuantity,
            BigDecimal originalRate,
            BigDecimal newRate,
            BigDecimal unitRate,
            BigDecimal lineAmountExGst
    ) {
        public static LineItemDto from(ChangeOrderLineItem li) {
            return new LineItemDto(
                    li.getId(), li.getDescription(), li.getUnit(),
                    li.getOriginalQuantity(), li.getNewQuantity(), li.getDeltaQuantity(),
                    li.getOriginalRate(), li.getNewRate(), li.getUnitRate(),
                    li.getLineAmountExGst()
            );
        }
    }

    public static ChangeOrderResponse from(ChangeOrder co) {
        List<LineItemDto> items = co.getLineItems() == null ? List.of() :
                co.getLineItems().stream().map(LineItemDto::from).toList();

        return new ChangeOrderResponse(
                co.getId(),
                co.getProject() != null ? co.getProject().getId() : null,
                co.getBoqDocument() != null ? co.getBoqDocument().getId() : null,
                co.getReferenceNumber(),
                co.getCoType() != null ? co.getCoType().name() : null,
                co.getStatus() != null ? co.getStatus().name() : null,
                co.getTitle(),
                co.getDescription(),
                co.getJustification(),
                co.getNetAmountExGst(),
                co.getGstRate(),
                co.getGstAmount(),
                co.getNetAmountInclGst(),
                co.getSubmittedAt(),
                co.getInternallyApprovedAt(),
                co.getInternallyApprovedBy(),
                co.getApprovedAt(),
                co.getRejectedAt(),
                co.getRejectionReason(),
                co.getReviewDeadline(),
                co.getCreatedAt(),
                items
        );
    }
}
