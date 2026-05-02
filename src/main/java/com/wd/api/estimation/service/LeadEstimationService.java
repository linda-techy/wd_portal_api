package com.wd.api.estimation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.EstimationLineItem;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.dto.*;
import com.wd.api.estimation.repository.EstimationLineItemRepository;
import com.wd.api.estimation.repository.EstimationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LeadEstimationService {

    private final EstimationPreviewService previewService;
    private final EstimationRepository estimationRepo;
    private final EstimationLineItemRepository lineItemRepo;
    private final ObjectMapper objectMapper;

    public LeadEstimationService(
            EstimationPreviewService previewService,
            EstimationRepository estimationRepo,
            EstimationLineItemRepository lineItemRepo,
            ObjectMapper objectMapper) {
        this.previewService = previewService;
        this.estimationRepo = estimationRepo;
        this.lineItemRepo = lineItemRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LeadEstimationDetailResponse create(LeadEstimationCreateRequest req) {
        // Compute via the existing preview service. This pins the active rate-version
        // and market-index at compute time, returning their IDs in the response.
        CalculatePreviewResponse preview = previewService.preview(req.preview());

        Estimation est = new Estimation();
        est.setEstimationNo(generateEstimationNo());
        est.setLeadId(req.leadId());
        est.setProjectType(req.preview().projectType());
        est.setPackageId(req.preview().packageId());
        est.setRateVersionId(preview.rateVersionId());
        est.setMarketIndexId(preview.marketIndexId());
        est.setDimensionsJson(toDimensionsMap(req.preview().dimensions()));
        est.setStatus(EstimationStatus.DRAFT);
        est.setSubtotal(preview.subtotal());
        est.setDiscountAmount(preview.discount());
        est.setGstAmount(preview.gst());
        est.setGrandTotal(preview.grandTotal());
        est.setValidUntil(req.validUntil() != null
                ? req.validUntil()
                : LocalDate.now().plusDays(30));

        Estimation saved = estimationRepo.save(est);

        // Persist each line item from the preview response.
        for (LineItemDto li : preview.lineItems()) {
            EstimationLineItem row = new EstimationLineItem();
            row.setEstimationId(saved.getId());
            row.setLineType(li.lineType());
            row.setDescription(li.description());
            row.setSourceRefId(li.sourceRefId());
            row.setQuantity(li.quantity());
            row.setUnit(li.unit());
            row.setUnitRate(li.unitRate());
            row.setAmount(li.amount());
            row.setDisplayOrder(li.displayOrder());
            lineItemRepo.save(row);
        }

        return LeadEstimationDetailResponse.fromEntity(saved, preview.lineItems());
    }

    @Transactional(readOnly = true)
    public List<LeadEstimationSummaryResponse> listByLead(Long leadId) {
        return estimationRepo.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(LeadEstimationSummaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeadEstimationDetailResponse get(UUID id) {
        Estimation est = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        // Re-fetch line items rather than recomputing — preserves the pinned rates.
        List<LineItemDto> lineItems = lineItemRepo
                .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                .map(li -> new LineItemDto(
                        li.getLineType(), li.getDescription(), li.getSourceRefId(),
                        li.getQuantity(), li.getUnit(), li.getUnitRate(),
                        li.getAmount(), li.getDisplayOrder()))
                .toList();
        return LeadEstimationDetailResponse.fromEntity(est, lineItems);
    }

    @Transactional
    public void delete(UUID id) {
        Estimation est = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        estimationRepo.delete(est);  // Soft-delete via @SQLDelete on BaseEntity
    }

    private String generateEstimationNo() {
        // Format: EST-{yyyyMM}-{6 hex chars from UUID}. Must fit VARCHAR(30).
        return "EST-"
                + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Convert DimensionsDto to a Map so it can be stored as JSONB via
     * Estimation.dimensionsJson (typed Map<String, Object> with @JdbcTypeCode(JSON)).
     * Uses the Spring-managed ObjectMapper bean for consistent serialization.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toDimensionsMap(DimensionsDto dim) {
        try {
            return objectMapper.convertValue(dim, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert dimensions to map", e);
        }
    }
}
