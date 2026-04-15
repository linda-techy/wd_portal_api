package com.wd.api.dto;

import com.wd.api.model.BoqInvoice;
import com.wd.api.model.BoqInvoiceLineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BoqInvoiceResponse(
        Long id,
        Long projectId,
        String invoiceType,
        String invoiceNumber,
        Long paymentStageId,
        Long changeOrderId,
        String coBillingEvent,
        BigDecimal subtotalExGst,
        BigDecimal gstRate,
        BigDecimal gstAmount,
        BigDecimal totalInclGst,
        BigDecimal totalCreditApplied,
        BigDecimal netAmountDue,
        String status,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String paymentReference,
        LocalDateTime createdAt,
        List<LineItemDto> lineItems
) {

    public record LineItemDto(String lineType, String description, BigDecimal amount, int sortOrder) {
        public static LineItemDto from(BoqInvoiceLineItem li) {
            return new LineItemDto(li.getLineType(), li.getDescription(), li.getAmount(), li.getSortOrder());
        }
    }

    public static BoqInvoiceResponse from(BoqInvoice inv) {
        List<LineItemDto> lines = inv.getLineItems() == null ? List.of() :
                inv.getLineItems().stream().map(LineItemDto::from).toList();

        return new BoqInvoiceResponse(
                inv.getId(),
                inv.getProject() != null ? inv.getProject().getId() : null,
                inv.getInvoiceType(),
                inv.getInvoiceNumber(),
                inv.getPaymentStage() != null ? inv.getPaymentStage().getId() : null,
                inv.getChangeOrder() != null ? inv.getChangeOrder().getId() : null,
                inv.getCoBillingEvent(),
                inv.getSubtotalExGst(),
                inv.getGstRate(),
                inv.getGstAmount(),
                inv.getTotalInclGst(),
                inv.getTotalCreditApplied(),
                inv.getNetAmountDue(),
                inv.getStatus() != null ? inv.getStatus().name() : null,
                inv.getIssueDate(),
                inv.getDueDate(),
                inv.getPaidAt(),
                inv.getPaymentReference(),
                inv.getCreatedAt(),
                lines
        );
    }
}
