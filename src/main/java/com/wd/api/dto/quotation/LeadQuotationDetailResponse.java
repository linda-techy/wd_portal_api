package com.wd.api.dto.quotation;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LeadQuotationDetailResponse(
        Long id,
        Long leadId,
        String quotationNumber,
        Integer version,
        String title,
        String description,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal taxRatePercent,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Integer validityDays,
        String status,
        String pricingMode,
        BigDecimal ratePerSqft,
        LocalDateTime sentAt,
        LocalDateTime viewedAt,
        LocalDateTime respondedAt,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String notes,
        List<Item> items
) {
    public record Item(
            Long id,
            Long quotationId,
            Integer itemNumber,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String notes,
            Long catalogItemId
    ) {
        public static Item from(LeadQuotationItem i) {
            return new Item(
                    i.getId(),
                    i.getQuotation() != null ? i.getQuotation().getId() : null,
                    i.getItemNumber(),
                    i.getDescription(),
                    i.getQuantity(),
                    i.getUnitPrice(),
                    i.getTotalPrice(),
                    i.getNotes(),
                    i.getCatalogItem() != null ? i.getCatalogItem().getId() : null
            );
        }
    }

    public static LeadQuotationDetailResponse from(LeadQuotation q) {
        return new LeadQuotationDetailResponse(
                q.getId(),
                q.getLeadId(),
                q.getQuotationNumber(),
                q.getVersion(),
                q.getTitle(),
                q.getDescription(),
                q.getTotalAmount(),
                q.getTaxAmount(),
                q.getTaxRatePercent(),
                q.getDiscountAmount(),
                q.getFinalAmount(),
                q.getValidityDays(),
                q.getStatus(),
                q.getPricingMode(),
                q.getRatePerSqft(),
                q.getSentAt(),
                q.getViewedAt(),
                q.getRespondedAt(),
                q.getCreatedById(),
                q.getCreatedAt(),
                q.getUpdatedAt(),
                q.getNotes(),
                q.getItems().stream().map(Item::from).toList()
        );
    }
}
