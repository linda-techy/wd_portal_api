package com.wd.api.estimation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.EstimationLineItem;
import com.wd.api.estimation.domain.enums.DiscountApprovalStatus;
import com.wd.api.estimation.domain.enums.EstimationPricingMode;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.dto.*;
import com.wd.api.estimation.repository.*;
import com.wd.api.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeadEstimationService {

    @Value("${estimation.discount.approval-threshold-percent:0.05}")
    private java.math.BigDecimal discountApprovalThreshold;

    private final EstimationPreviewService previewService;
    private final EstimationRepository estimationRepo;
    private final EstimationLineItemRepository lineItemRepo;
    private final EstimationInclusionRepository inclusionRepo;
    private final EstimationExclusionRepository exclusionRepo;
    private final EstimationAssumptionRepository assumptionRepo;
    private final EstimationPaymentMilestoneRepository milestoneRepo;
    private final LeadRepository leadRepo;
    private final ObjectMapper objectMapper;

    public LeadEstimationService(
            EstimationPreviewService previewService,
            EstimationRepository estimationRepo,
            EstimationLineItemRepository lineItemRepo,
            EstimationInclusionRepository inclusionRepo,
            EstimationExclusionRepository exclusionRepo,
            EstimationAssumptionRepository assumptionRepo,
            EstimationPaymentMilestoneRepository milestoneRepo,
            LeadRepository leadRepo,
            ObjectMapper objectMapper) {
        this.previewService = previewService;
        this.estimationRepo = estimationRepo;
        this.lineItemRepo = lineItemRepo;
        this.inclusionRepo = inclusionRepo;
        this.exclusionRepo = exclusionRepo;
        this.assumptionRepo = assumptionRepo;
        this.milestoneRepo = milestoneRepo;
        this.leadRepo = leadRepo;
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
        est.setPricingMode(preview.pricingMode());
        est.setStatus(EstimationStatus.DRAFT);
        est.setValidUntil(req.validUntil() != null
                ? req.validUntil()
                : LocalDate.now().plusDays(30));

        if (preview.pricingMode() == EstimationPricingMode.BUDGETARY) {
            est.setEstimatedAreaSqft(preview.estimatedAreaSqft());
            est.setGrandTotalMin(preview.grandTotalMin());
            est.setGrandTotalMax(preview.grandTotalMax());
            est.setGrandTotal(preview.grandTotalMin());
            // P — persist sales-set confidence (defaults to MEDIUM when omitted upstream).
            est.setConfidenceLevel(req.preview().confidenceLevel() != null
                    ? req.preview().confidenceLevel()
                    : com.wd.api.estimation.domain.enums.EstimationConfidenceLevel.MEDIUM);
            // dimensions_json is NOT NULL at the DB layer; budgetary stores an empty object.
            est.setDimensionsJson(Map.of());
        } else {
            est.setDimensionsJson(toDimensionsMap(req.preview().dimensions()));
            est.setSubtotal(preview.subtotal());
            est.setDiscountAmount(preview.discount());
            est.setGstAmount(preview.gst());
            est.setGrandTotal(preview.grandTotal());
        }

        // O — record discount % from the preview request and gate the row if above threshold.
        java.math.BigDecimal reqDiscount = req.preview().discountPercent();
        if (reqDiscount != null) {
            est.setDiscountPercent(reqDiscount);
            if (reqDiscount.compareTo(discountApprovalThreshold) > 0) {
                est.setDiscountApprovalStatus(DiscountApprovalStatus.PENDING);
            }
        }

        Estimation saved = estimationRepo.save(est);

        // L — newly created estimation becomes current (clears any sibling).
        setCurrentEstimation(saved.getId(), saved.getLeadId());

        // Line items only exist for LINE_ITEM mode.
        if (preview.pricingMode() == EstimationPricingMode.LINE_ITEM) {
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
        }

        return LeadEstimationDetailResponse.fromEntity(saved, preview.lineItems(),
                List.of(), List.of(), List.of(), List.of());
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
        List<EstimationSubResourceResponse> inclusions = inclusionRepo
                .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                        e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                .toList();
        List<EstimationSubResourceResponse> exclusions = exclusionRepo
                .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                        e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                .toList();
        List<EstimationSubResourceResponse> assumptions = assumptionRepo
                .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                        e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                .toList();
        List<EstimationSubResourceResponse> paymentMilestones = milestoneRepo
                .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                        e.getLabel(), e.getDescription(), e.getDisplayOrder(), e.getPercentage()))
                .toList();
        return LeadEstimationDetailResponse.fromEntity(est, lineItems,
                inclusions, exclusions, assumptions, paymentMilestones);
    }

    @Transactional
    public LeadEstimationDetailResponse markSent(UUID id) {
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getStatus() != EstimationStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only mark DRAFT estimations as SENT (current: " + e.getStatus() + ")");
        }
        // O — block send when discount above threshold hasn't been approved yet.
        DiscountApprovalStatus approval = e.getDiscountApprovalStatus();
        if (approval == DiscountApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    "Discount above " + discountApprovalThreshold.multiply(new java.math.BigDecimal("100")).stripTrailingZeros().toPlainString()
                            + "% requires manager approval before sending.");
        }
        if (approval == DiscountApprovalStatus.REJECTED) {
            throw new IllegalStateException(
                    "Discount approval was rejected. Revise the estimation with a lower discount before sending.");
        }
        e.setStatus(EstimationStatus.SENT);
        estimationRepo.save(e);
        return get(id);
    }

    @Transactional
    public LeadEstimationDetailResponse approveDiscount(UUID id, Long approverUserId, String notes) {
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getDiscountApprovalStatus() == null) {
            throw new IllegalStateException("Discount on this estimation does not require approval.");
        }
        if (e.getDiscountApprovalStatus() == DiscountApprovalStatus.APPROVED) {
            throw new IllegalStateException("Discount is already approved.");
        }
        e.setDiscountApprovalStatus(DiscountApprovalStatus.APPROVED);
        e.setDiscountApprovedByUserId(approverUserId);
        e.setDiscountApprovedAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.isBlank()) e.setDiscountApprovalNotes(notes);
        estimationRepo.save(e);
        return get(id);
    }

    @Transactional
    public LeadEstimationDetailResponse rejectDiscount(UUID id, Long approverUserId, String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("A reason is required when rejecting a discount.");
        }
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getDiscountApprovalStatus() == null) {
            throw new IllegalStateException("Discount on this estimation does not require approval.");
        }
        e.setDiscountApprovalStatus(DiscountApprovalStatus.REJECTED);
        e.setDiscountApprovedByUserId(approverUserId);
        e.setDiscountApprovedAt(java.time.LocalDateTime.now());
        e.setDiscountApprovalNotes(notes);
        estimationRepo.save(e);
        return get(id);
    }

    @Transactional
    public LeadEstimationDetailResponse markAccepted(UUID id) {
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getStatus() != EstimationStatus.SENT) {
            throw new IllegalStateException(
                    "Can only mark SENT estimations as ACCEPTED (current: " + e.getStatus() + ")");
        }
        e.setStatus(EstimationStatus.ACCEPTED);
        estimationRepo.save(e);
        // L — ACCEPTED becomes the current estimation (overrides any sibling).
        setCurrentEstimation(e.getId(), e.getLeadId());
        // On accepted, flip the lead's status to project_won (best-effort).
        leadRepo.findById(e.getLeadId()).ifPresent(lead -> {
            if (!"project_won".equalsIgnoreCase(lead.getLeadStatus())) {
                lead.setLeadStatus("project_won");
                leadRepo.save(lead);
            }
        });
        return get(id);
    }

    @Transactional
    public LeadEstimationDetailResponse markRejected(UUID id) {
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getStatus() != EstimationStatus.SENT) {
            throw new IllegalStateException(
                    "Can only mark SENT estimations as REJECTED (current: " + e.getStatus() + ")");
        }
        e.setStatus(EstimationStatus.REJECTED);
        estimationRepo.save(e);
        return get(id);
    }

    @Transactional
    public LeadEstimationDetailResponse revertToDraft(UUID id) {
        Estimation e = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        if (e.getStatus() == EstimationStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot revert ACCEPTED estimations to DRAFT");
        }
        e.setStatus(EstimationStatus.DRAFT);
        estimationRepo.save(e);
        return get(id);
    }

    @Transactional
    public void delete(UUID id) {
        Estimation est = estimationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + id));
        boolean wasCurrent = est.isCurrent();
        Long leadId = est.getLeadId();
        estimationRepo.delete(est);  // Soft-delete via @SQLDelete on BaseEntity
        // L — if we just removed the current estimation, promote the next-most-recent.
        if (wasCurrent) {
            estimationRepo.findActiveByLeadOrderByCreatedAtDesc(leadId).stream()
                    .filter(e -> !e.getId().equals(id))
                    .findFirst()
                    .ifPresent(replacement -> setCurrentEstimation(replacement.getId(), leadId));
        }
    }

    /**
     * L — atomically marks one estimation as current within a lead. Clears any
     * existing current first to satisfy the partial-unique index.
     */
    private void setCurrentEstimation(UUID estimationId, Long leadId) {
        estimationRepo.clearCurrentForLead(leadId);
        estimationRepo.markCurrent(estimationId);
    }

    @Transactional(readOnly = true)
    public Optional<PublicEstimationResponse> getByPublicToken(UUID token) {
        return estimationRepo.findByPublicViewToken(token).map(est -> {
            List<LineItemDto> lineItems = lineItemRepo
                    .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                    .map(li -> new LineItemDto(
                            li.getLineType(), li.getDescription(), li.getSourceRefId(),
                            li.getQuantity(), li.getUnit(), li.getUnitRate(),
                            li.getAmount(), li.getDisplayOrder()))
                    .toList();
            List<EstimationSubResourceResponse> inclusions = inclusionRepo
                    .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                    .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                            e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                    .toList();
            List<EstimationSubResourceResponse> exclusions = exclusionRepo
                    .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                    .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                            e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                    .toList();
            List<EstimationSubResourceResponse> assumptions = assumptionRepo
                    .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                    .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                            e.getLabel(), e.getDescription(), e.getDisplayOrder(), null))
                    .toList();
            List<EstimationSubResourceResponse> paymentMilestones = milestoneRepo
                    .findByEstimationIdOrderByDisplayOrderAsc(est.getId()).stream()
                    .map(e -> new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                            e.getLabel(), e.getDescription(), e.getDisplayOrder(), e.getPercentage()))
                    .toList();
            return new PublicEstimationResponse(
                    est.getId(), est.getEstimationNo(), est.getProjectType(),
                    est.getStatus().name(),
                    est.getSubtotal(), est.getDiscountAmount(), est.getGstAmount(),
                    est.getGrandTotal(), est.getValidUntil(), est.getCreatedAt(),
                    lineItems, inclusions, exclusions, assumptions, paymentMilestones,
                    est.getPricingMode(), est.getEstimatedAreaSqft(),
                    est.getGrandTotalMin(), est.getGrandTotalMax());
        });
    }

    @Transactional
    public LeadEstimationDetailResponse revise(UUID parentId, LeadEstimationCreateRequest req) {
        Estimation parent = estimationRepo.findById(parentId).orElseThrow(() ->
                new IllegalArgumentException("Estimation not found: " + parentId));
        if (parent.getStatus() != EstimationStatus.DRAFT && parent.getStatus() != EstimationStatus.SENT) {
            throw new IllegalStateException(
                    "Can only revise DRAFT or SENT estimations (parent is " + parent.getStatus() + ")");
        }
        if (!parent.getLeadId().equals(req.leadId())) {
            throw new IllegalArgumentException("Cannot reassign lead via revision");
        }
        // Reuse existing create() flow which builds an Estimation + line items
        LeadEstimationDetailResponse created = create(req);
        // Backfill parent link
        Estimation child = estimationRepo.findById(created.id()).orElseThrow();
        child.setParentEstimationId(parent.getId());
        estimationRepo.save(child);
        return get(created.id());
    }

    @Transactional
    public LeadEstimationDetailResponse regeneratePublicToken(UUID estimationId) {
        Estimation e = estimationRepo.findById(estimationId)
                .orElseThrow(() -> new IllegalArgumentException("Estimation not found: " + estimationId));
        e.setPublicViewToken(UUID.randomUUID());
        estimationRepo.save(e);
        return get(estimationId);
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
