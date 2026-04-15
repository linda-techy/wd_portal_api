package com.wd.api.service;

import com.wd.api.dto.VariationOrderDtos.*;
import com.wd.api.event.VOApprovedEvent;
import com.wd.api.model.*;
import com.wd.api.model.enums.*;
import com.wd.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the full Variation Order (VO) lifecycle:
 *
 *   DRAFT → SUBMITTED → (multi-level approval) → APPROVED / REJECTED
 *
 * Approval rules:
 *   approved_cost < ₹7.5L    → PM can approve
 *   ₹7.5L–₹37.5L            → COMMERCIAL_MANAGER required
 *   > ₹37.5L                 → DIRECTOR required
 *
 * On final approval: auto-creates a ChangeOrderPaymentSchedule from the VO category,
 * then publishes VOApprovedEvent.
 */
@Service
@Transactional
public class VariationOrderService {

    private static final Logger logger = LoggerFactory.getLogger(VariationOrderService.class);

    private final ChangeOrderRepository changeOrderRepository;
    private final ChangeOrderApprovalHistoryRepository approvalHistoryRepository;
    private final ChangeOrderPaymentScheduleRepository paymentScheduleRepository;
    private final BoqDocumentRepository boqDocumentRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository portalUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VariationOrderService(
            ChangeOrderRepository changeOrderRepository,
            ChangeOrderApprovalHistoryRepository approvalHistoryRepository,
            ChangeOrderPaymentScheduleRepository paymentScheduleRepository,
            BoqDocumentRepository boqDocumentRepository,
            CustomerProjectRepository projectRepository,
            PortalUserRepository portalUserRepository,
            ApplicationEventPublisher eventPublisher) {
        this.changeOrderRepository   = changeOrderRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.boqDocumentRepository   = boqDocumentRepository;
        this.projectRepository       = projectRepository;
        this.portalUserRepository    = portalUserRepository;
        this.eventPublisher          = eventPublisher;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ChangeOrder getVariationOrder(Long id) {
        return changeOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Variation order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChangeOrder> listByProject(Long projectId) {
        return changeOrderRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<ChangeOrderApprovalHistory> getApprovalHistory(Long changeOrderId) {
        return approvalHistoryRepository.findByChangeOrderIdOrderByActionAtDesc(changeOrderId);
    }

    @Transactional(readOnly = true)
    public ChangeOrderPaymentSchedule getPaymentSchedule(Long changeOrderId) {
        return paymentScheduleRepository.findByChangeOrderId(changeOrderId)
                .orElseThrow(() -> new IllegalStateException(
                        "No payment schedule found for VO " + changeOrderId +
                        ". VO must be APPROVED first."));
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public ChangeOrder createDraft(Long projectId, CreateVariationOrderRequest req, Long userId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        BoqDocument approvedBoq = boqDocumentRepository.findApprovedByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot create a VO — project " + projectId + " has no approved BOQ."));

        ChangeOrder co = new ChangeOrder();
        co.setProject(project);
        co.setBoqDocument(approvedBoq);
        co.setCoType(ChangeOrderType.valueOf(req.coType()));
        co.setTitle(req.title());
        co.setDescription(req.description());
        co.setJustification(req.justification());
        co.setScopeNotes(req.scopeNotes());
        co.setVoCategory(req.voCategory());
        co.setMappedStageIds(req.mappedStageIds());
        co.setReviewDeadline(req.reviewDeadline());
        co.setStatus(ChangeOrderStatus.DRAFT);

        if (req.revisesCoId() != null) {
            ChangeOrder revises = changeOrderRepository.findById(req.revisesCoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Revised CO not found: " + req.revisesCoId()));
            co.setRevisesChangeOrder(revises);
        }

        BigDecimal gstRate = req.gstRate() != null ? req.gstRate() : new BigDecimal("0.18");
        co.setNetAmountExGst(req.netAmountExGst());
        co.setGstRate(gstRate);
        BigDecimal gst = req.netAmountExGst().multiply(gstRate).setScale(6, RoundingMode.HALF_UP);
        co.setGstAmount(gst);
        co.setNetAmountInclGst(req.netAmountExGst().add(gst));

        // Reference number: CO-<projectId>-<count+1>
        long count = changeOrderRepository.countByProjectIdAndDeletedAtIsNull(projectId);
        co.setReferenceNumber("VO-" + projectId + "-" + String.format("%03d", count + 1));

        PortalUser creator = portalUserRepository.findById(userId).orElse(null);
        co.setSubmittedBy(creator);

        if (req.lineItems() != null) {
            for (LineItemRequest li : req.lineItems()) {
                ChangeOrderLineItem item = buildLineItem(co, li);
                co.getLineItems().add(item);
            }
        }

        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Submit for approval
    // -------------------------------------------------------------------------

    public ChangeOrder submit(Long changeOrderId, Long userId) {
        ChangeOrder co = requireDraft(changeOrderId);
        co.setStatus(ChangeOrderStatus.SUBMITTED);
        co.setSubmittedAt(LocalDateTime.now());
        PortalUser submitter = portalUserRepository.findById(userId).orElse(null);
        co.setSubmittedBy(submitter);
        logger.info("VO {} submitted by user {}", co.getReferenceNumber(), userId);
        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Multi-level approval
    // -------------------------------------------------------------------------

    /**
     * Process an approval action at the given level.
     *
     * Rules enforced:
     * - VO must be SUBMITTED (or CUSTOMER_REVIEW for backward compat).
     * - Approver's level must satisfy the threshold for the VO cost.
     * - APPROVED at sufficient level → creates payment schedule, publishes event.
     * - REJECTED → terminal.
     * - ESCALATED → records audit row; calling controller should re-route to next level.
     * - RETURNED  → back to DRAFT for amendment.
     */
    public ChangeOrder processApproval(Long changeOrderId, VOApprovalRequest req,
                                       ApprovalLevel approverLevel, Long approverId, String approverName) {
        ChangeOrder co = changeOrderRepository.findById(changeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("VO not found: " + changeOrderId));

        if (co.getStatus() != ChangeOrderStatus.SUBMITTED
                && co.getStatus() != ChangeOrderStatus.CUSTOMER_REVIEW) {
            throw new IllegalStateException(
                    "VO must be SUBMITTED to approve. Current status: " + co.getStatus());
        }

        BigDecimal cost = co.getApprovedCost() != null ? co.getApprovedCost() : co.getNetAmountInclGst();
        ApprovalLevel required = ApprovalLevel.requiredFor(cost);

        // Record history row (immutable)
        PortalUser approver = portalUserRepository.findById(approverId).orElse(null);
        ChangeOrderApprovalHistory history = new ChangeOrderApprovalHistory();
        history.setChangeOrder(co);
        history.setApprover(approver);
        history.setApproverName(approverName);
        history.setLevel(approverLevel);
        history.setAction(req.action());
        history.setComment(req.comment());
        approvalHistoryRepository.save(history);

        switch (req.action()) {
            case APPROVED -> {
                if (!approverLevel.canApprove(required)) {
                    throw new IllegalStateException(
                            "Your level (" + approverLevel + ") cannot approve a VO requiring " + required);
                }
                co.setStatus(ChangeOrderStatus.APPROVED);
                co.setApprovedAt(LocalDateTime.now());
                co.setApprovedBy(approverId);
                if (co.getApprovedCost() == null) co.setApprovedCost(co.getNetAmountInclGst());
                changeOrderRepository.save(co);
                autoCreatePaymentSchedule(co);
                eventPublisher.publishEvent(
                        new VOApprovedEvent(this, co.getId(), co.getProject().getId(), approverId));
                logger.info("VO {} APPROVED by {} at level {}", co.getReferenceNumber(), approverName, approverLevel);
            }
            case REJECTED -> {
                co.setStatus(ChangeOrderStatus.REJECTED);
                co.setRejectedAt(LocalDateTime.now());
                co.setRejectedBy(approverId);
                co.setRejectionReason(req.comment());
                changeOrderRepository.save(co);
                logger.info("VO {} REJECTED by {}", co.getReferenceNumber(), approverName);
            }
            case RETURNED -> {
                co.setStatus(ChangeOrderStatus.DRAFT);
                changeOrderRepository.save(co);
                logger.info("VO {} RETURNED to draft by {}", co.getReferenceNumber(), approverName);
            }
            case ESCALATED -> {
                // Status stays SUBMITTED — the event/notification system will re-route
                changeOrderRepository.save(co);
                logger.info("VO {} ESCALATED by {} to next level", co.getReferenceNumber(), approverName);
            }
        }

        return co;
    }

    // -------------------------------------------------------------------------
    // Payment schedule override
    // -------------------------------------------------------------------------

    public ChangeOrderPaymentSchedule updatePaymentSchedule(
            Long changeOrderId, UpdatePaymentScheduleRequest req) {

        ChangeOrder co = changeOrderRepository.findById(changeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("VO not found: " + changeOrderId));
        if (co.getStatus() != ChangeOrderStatus.APPROVED) {
            throw new IllegalStateException("Can only update payment schedule for an APPROVED VO.");
        }

        int total = req.advancePct() + req.progressPct() + req.completionPct();
        if (total != 100) {
            throw new IllegalArgumentException(
                    "Percentages must sum to 100. Got: " + total);
        }

        ChangeOrderPaymentSchedule schedule = paymentScheduleRepository.findByChangeOrderId(changeOrderId)
                .orElseGet(() -> {
                    ChangeOrderPaymentSchedule s = new ChangeOrderPaymentSchedule();
                    s.setChangeOrder(co);
                    return s;
                });

        schedule.setAdvancePct(req.advancePct());
        schedule.setProgressPct(req.progressPct());
        schedule.setCompletionPct(req.completionPct());
        if (req.advanceDueDate() != null)   schedule.setAdvanceDueDate(req.advanceDueDate());
        if (req.completionTrigger() != null) schedule.setCompletionTrigger(req.completionTrigger());
        if (req.progressTriggerStageId() != null) {
            // Lazy ref — just store id via a lightweight lookup is fine here
            schedule.setProgressTriggerStage(null); // cleared; set via repo if needed
        }
        schedule.recomputeAmounts(co.getApprovedCost());
        return paymentScheduleRepository.save(schedule);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void autoCreatePaymentSchedule(ChangeOrder co) {
        if (paymentScheduleRepository.existsByChangeOrderId(co.getId())) {
            logger.debug("Payment schedule already exists for VO {}", co.getId());
            return;
        }
        ChangeOrderPaymentSchedule schedule = new ChangeOrderPaymentSchedule();
        schedule.setChangeOrder(co);
        schedule.applyCategory(co.getVoCategory(), co.getApprovedCost());
        paymentScheduleRepository.save(schedule);
        logger.info("Auto-created payment schedule for VO {} (category: {})",
                co.getReferenceNumber(), co.getVoCategory());
    }

    private ChangeOrder requireDraft(Long id) {
        ChangeOrder co = changeOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VO not found: " + id));
        if (co.getStatus() != ChangeOrderStatus.DRAFT) {
            throw new IllegalStateException("VO must be in DRAFT status. Current: " + co.getStatus());
        }
        return co;
    }

    private ChangeOrderLineItem buildLineItem(ChangeOrder co, LineItemRequest req) {
        ChangeOrderLineItem item = new ChangeOrderLineItem();
        item.setChangeOrder(co);
        item.setDescription(req.description());
        item.setUnit(req.unit());
        item.setOriginalQuantity(req.originalQuantity() != null ? req.originalQuantity() : BigDecimal.ZERO);
        item.setNewQuantity(req.newQuantity() != null ? req.newQuantity() : BigDecimal.ZERO);
        item.setOriginalRate(req.originalRate() != null ? req.originalRate() : BigDecimal.ZERO);
        item.setNewRate(req.newRate() != null ? req.newRate() : BigDecimal.ZERO);
        item.setLineAmountExGst(req.lineAmountExGst());
        return item;
    }
}
