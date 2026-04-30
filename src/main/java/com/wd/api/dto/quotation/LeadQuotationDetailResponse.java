package com.wd.api.dto.quotation;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full read DTO for a single quotation. Composes the parent header with the
 * line items and the four V76 child collections (inclusions, exclusions,
 * assumptions, payment milestones), so the Flutter detail screen can render
 * the entire 3-stage view from a single GET.
 *
 * <p>The new fields default to {@code null} for legacy rows — the BUDGETARY
 * stage in particular leaves grand totals null on purpose, and the Flutter
 * side is expected to suppress total widgets when {@link #showGrandTotal}
 * is {@code false}.
 */
public record LeadQuotationDetailResponse(
        Long id,
        Long leadId,
        String quotationNumber,
        Integer version,
        String title,
        String description,
        // Stage discriminator + parent link (V76)
        String quotationType,
        Long parentQuotationId,
        String tier,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal taxRatePercent,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        Integer validityDays,
        // Absolute expiry date (V76) — replaces validityDays as source of truth
        LocalDate validUntil,
        boolean showGrandTotal,
        String status,
        String pricingMode,
        BigDecimal ratePerSqft,
        // Tier-aware rate range + estimate ranges (V76)
        BigDecimal ratePerSqftMin,
        BigDecimal ratePerSqftMax,
        BigDecimal estimatedAreaMin,
        BigDecimal estimatedAreaMax,
        Integer durationMonthsMin,
        Integer durationMonthsMax,
        UUID publicViewToken,
        LocalDateTime sentAt,
        LocalDateTime viewedAt,
        LocalDateTime respondedAt,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String notes,
        List<Item> items,
        // V76 sub-resources
        List<InclusionResponse> inclusions,
        List<ExclusionResponse> exclusions,
        List<AssumptionResponse> assumptions,
        List<PaymentMilestoneResponse> paymentMilestones
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
                q.getQuotationType(),
                q.getParentQuotationId(),
                q.getTier(),
                q.getTotalAmount(),
                q.getTaxAmount(),
                q.getTaxRatePercent(),
                q.getDiscountAmount(),
                q.getFinalAmount(),
                q.getValidityDays(),
                q.getValidUntil(),
                q.isShowGrandTotal(),
                q.getStatus(),
                q.getPricingMode(),
                q.getRatePerSqft(),
                q.getRatePerSqftMin(),
                q.getRatePerSqftMax(),
                q.getEstimatedAreaMin(),
                q.getEstimatedAreaMax(),
                q.getDurationMonthsMin(),
                q.getDurationMonthsMax(),
                q.getPublicViewToken(),
                q.getSentAt(),
                q.getViewedAt(),
                q.getRespondedAt(),
                q.getCreatedById(),
                q.getCreatedAt(),
                q.getUpdatedAt(),
                q.getNotes(),
                q.getItems().stream().map(Item::from).toList(),
                q.getInclusions().stream().map(InclusionResponse::from).toList(),
                q.getExclusions().stream().map(ExclusionResponse::from).toList(),
                q.getAssumptions().stream().map(AssumptionResponse::from).toList(),
                q.getPaymentMilestones().stream().map(PaymentMilestoneResponse::from).toList()
        );
    }
}
