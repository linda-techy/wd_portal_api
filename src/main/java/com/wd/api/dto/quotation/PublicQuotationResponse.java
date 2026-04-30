package com.wd.api.dto.quotation;

import com.wd.api.model.LeadQuotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer-facing read DTO returned by the public token-gated endpoint.
 *
 * <p>Deliberately narrower than {@link LeadQuotationDetailResponse}:
 *
 * <ul>
 *   <li><b>No internal IDs</b> beyond the parent quotation id (no leadId,
 *       no createdById, no item ids) — customers should not see
 *       internal references that could be guessed.</li>
 *   <li><b>Total fields are conditionally suppressed</b> — when
 *       {@code showGrandTotal} is {@code false} (BUDGETARY default), every
 *       grand-total figure is null, including line item rupees. The
 *       customer sees scope, ranges, and CTA — not a number to negotiate
 *       down.</li>
 *   <li><b>Status is exposed but only when SENT or later</b> — a customer
 *       hitting a token for a DRAFT quote (e.g. staff shared a preview by
 *       accident) sees {@code status = "PREVIEW"} instead.</li>
 * </ul>
 */
public record PublicQuotationResponse(
        Long id,
        String quotationNumber,
        Integer version,
        String quotationType,
        String tier,
        String title,
        String description,
        boolean showGrandTotal,
        // Range fields — always shown
        BigDecimal ratePerSqftMin,
        BigDecimal ratePerSqftMax,
        BigDecimal estimatedAreaMin,
        BigDecimal estimatedAreaMax,
        Integer durationMonthsMin,
        Integer durationMonthsMax,
        // Grand-total fields — null unless showGrandTotal=true
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal taxRatePercent,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        // Headline rate — only meaningful when SQFT_RATE pricing
        BigDecimal ratePerSqft,
        LocalDate validUntil,
        String status,
        // Sub-resources (always shown — these are the value prop)
        List<InclusionResponse> inclusions,
        List<ExclusionResponse> exclusions,
        List<AssumptionResponse> assumptions,
        List<PaymentMilestoneResponse> paymentMilestones,
        List<Item> items
) {
    public record Item(
            Integer itemNumber,
            String description,
            BigDecimal quantity,
            // Per-line totals also suppressed when showGrandTotal=false
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String notes
    ) {}

    public static PublicQuotationResponse from(LeadQuotation q) {
        boolean show = q.isShowGrandTotal();
        String safeStatus = isCustomerVisible(q.getStatus())
                ? q.getStatus()
                : "PREVIEW";
        return new PublicQuotationResponse(
                q.getId(),
                q.getQuotationNumber(),
                q.getVersion(),
                q.getQuotationType(),
                q.getTier(),
                q.getTitle(),
                q.getDescription(),
                show,
                q.getRatePerSqftMin(),
                q.getRatePerSqftMax(),
                q.getEstimatedAreaMin(),
                q.getEstimatedAreaMax(),
                q.getDurationMonthsMin(),
                q.getDurationMonthsMax(),
                show ? q.getTotalAmount() : null,
                show ? q.getTaxAmount() : null,
                q.getTaxRatePercent(),
                q.getDiscountAmount(),
                show ? q.getFinalAmount() : null,
                q.getRatePerSqft(),
                q.getValidUntil(),
                safeStatus,
                q.getInclusions().stream().map(InclusionResponse::from).toList(),
                q.getExclusions().stream().map(ExclusionResponse::from).toList(),
                q.getAssumptions().stream().map(AssumptionResponse::from).toList(),
                q.getPaymentMilestones().stream().map(PaymentMilestoneResponse::from).toList(),
                q.getItems().stream()
                        .map(i -> new Item(
                                i.getItemNumber(),
                                i.getDescription(),
                                i.getQuantity(),
                                show ? i.getUnitPrice() : null,
                                show ? i.getTotalPrice() : null,
                                i.getNotes()))
                        .toList()
        );
    }

    /**
     * Customer-visible statuses are anything past DRAFT — the customer should
     * not see internal lifecycle states like DRAFT spilling out via a leaked
     * token. (DRAFT was likely a preview shared by accident; mask as PREVIEW.)
     */
    private static boolean isCustomerVisible(String status) {
        if (status == null) return false;
        return switch (status) {
            case "SENT", "VIEWED", "ACCEPTED", "REJECTED", "EXPIRED" -> true;
            default -> false;
        };
    }
}
