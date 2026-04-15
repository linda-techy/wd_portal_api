package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.model.enums.ChangeOrderStatus;
import com.wd.api.model.enums.ChangeOrderType;
import com.wd.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the Change Order lifecycle.
 *
 * Rule R-003: Every scope change after BOQ approval is a Change Order.
 *
 * On approval of a REDUCTION CO  → auto-generates a CreditNote.
 * On approval of an ADDITION CO  → auto-generates a CO Invoice (ADVANCE event).
 */
@Service
@Transactional
public class ChangeOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ChangeOrderService.class);

    private final ChangeOrderRepository changeOrderRepository;
    private final BoqDocumentRepository boqDocumentRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository portalUserRepository;
    private final CreditNoteService creditNoteService;
    private final BoqInvoiceService boqInvoiceService;

    public ChangeOrderService(ChangeOrderRepository changeOrderRepository,
                               BoqDocumentRepository boqDocumentRepository,
                               CustomerProjectRepository projectRepository,
                               PortalUserRepository portalUserRepository,
                               CreditNoteService creditNoteService,
                               BoqInvoiceService boqInvoiceService) {
        this.changeOrderRepository = changeOrderRepository;
        this.boqDocumentRepository = boqDocumentRepository;
        this.projectRepository = projectRepository;
        this.portalUserRepository = portalUserRepository;
        this.creditNoteService = creditNoteService;
        this.boqInvoiceService = boqInvoiceService;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ChangeOrder getChangeOrder(Long id) {
        return changeOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Change order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChangeOrder> getProjectChangeOrders(Long projectId) {
        return changeOrderRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public ChangeOrder createChangeOrder(Long projectId, ChangeOrderType coType, String title,
                                          String description, String justification,
                                          List<ChangeOrderLineItem> lineItems,
                                          Long userId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        BoqDocument approvedBoq = boqDocumentRepository.findApprovedByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException(
                    "Cannot create a Change Order — project " + projectId + " has no approved BOQ."));

        String refNumber = generateReferenceNumber(projectId);

        ChangeOrder co = new ChangeOrder();
        co.setProject(project);
        co.setBoqDocument(approvedBoq);
        co.setReferenceNumber(refNumber);
        co.setCoType(coType);
        co.setTitle(title);
        co.setDescription(description);
        co.setJustification(justification);
        co.setCreatedByUserId(userId);

        // Attach line items
        for (ChangeOrderLineItem item : lineItems) {
            item.setChangeOrder(co);
        }
        co.setLineItems(lineItems);

        recalculateTotals(co);

        ChangeOrder saved = changeOrderRepository.save(co);
        logger.info("Created Change Order {} for project {}", refNumber, projectId);
        return saved;
    }

    // -------------------------------------------------------------------------
    // Submit for customer review
    // -------------------------------------------------------------------------

    public ChangeOrder submit(Long coId, Long userId) {
        ChangeOrder co = getChangeOrder(coId);

        if (!co.isDraft()) {
            throw new IllegalStateException("Only DRAFT change orders can be submitted. Current: " + co.getStatus());
        }

        PortalUser submitter = portalUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        co.setStatus(ChangeOrderStatus.SUBMITTED);
        co.setSubmittedAt(LocalDateTime.now());
        co.setSubmittedBy(submitter);
        co.setUpdatedByUserId(userId);
        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Mark as sent to customer for review
    // -------------------------------------------------------------------------

    public ChangeOrder sendToCustomer(Long coId, Long userId) {
        ChangeOrder co = getChangeOrder(coId);

        if (co.getStatus() != ChangeOrderStatus.SUBMITTED) {
            throw new IllegalStateException("CO must be SUBMITTED before sending to customer. Current: " + co.getStatus());
        }

        co.setStatus(ChangeOrderStatus.CUSTOMER_REVIEW);
        co.setCustomerReviewedAt(LocalDateTime.now());
        co.setUpdatedByUserId(userId);
        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Customer approval — triggers credit note or CO invoice
    // -------------------------------------------------------------------------

    public ChangeOrder approveByCustomer(Long coId, Long customerUserId) {
        ChangeOrder co = getChangeOrder(coId);

        if (co.getStatus() != ChangeOrderStatus.CUSTOMER_REVIEW) {
            throw new IllegalStateException("CO must be in CUSTOMER_REVIEW to approve. Current: " + co.getStatus());
        }

        co.setStatus(ChangeOrderStatus.APPROVED);
        co.setApprovedAt(LocalDateTime.now());
        co.setApprovedBy(customerUserId);
        ChangeOrder saved = changeOrderRepository.save(co);

        // Auto-generate downstream documents
        if (co.getCoType().isReduction()) {
            creditNoteService.generateForReduction(saved);
        } else if (co.getCoType().isAddition()) {
            boqInvoiceService.generateCoAdvanceInvoice(saved);
        }

        logger.info("Change Order {} approved by customer {}. Type: {}",
                co.getReferenceNumber(), customerUserId, co.getCoType());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Customer rejection
    // -------------------------------------------------------------------------

    public ChangeOrder rejectByCustomer(Long coId, Long customerUserId, String reason) {
        ChangeOrder co = getChangeOrder(coId);

        if (co.getStatus() != ChangeOrderStatus.CUSTOMER_REVIEW) {
            throw new IllegalStateException("CO must be in CUSTOMER_REVIEW to reject. Current: " + co.getStatus());
        }

        co.setStatus(ChangeOrderStatus.REJECTED);
        co.setRejectedAt(LocalDateTime.now());
        co.setRejectedBy(customerUserId);
        co.setRejectionReason(reason);
        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Progress tracking
    // -------------------------------------------------------------------------

    public ChangeOrder startProgress(Long coId, Long userId) {
        ChangeOrder co = getChangeOrder(coId);
        if (co.getStatus() != ChangeOrderStatus.APPROVED) {
            throw new IllegalStateException("CO must be APPROVED to start. Current: " + co.getStatus());
        }
        co.setStatus(ChangeOrderStatus.IN_PROGRESS);
        co.setUpdatedByUserId(userId);
        return changeOrderRepository.save(co);
    }

    public ChangeOrder complete(Long coId, Long userId) {
        ChangeOrder co = getChangeOrder(coId);
        if (co.getStatus() != ChangeOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("CO must be IN_PROGRESS to complete. Current: " + co.getStatus());
        }
        co.setStatus(ChangeOrderStatus.COMPLETED);
        co.setCompletedAt(LocalDateTime.now());
        co.setUpdatedByUserId(userId);
        return changeOrderRepository.save(co);
    }

    public ChangeOrder close(Long coId, Long userId) {
        ChangeOrder co = getChangeOrder(coId);
        if (co.getStatus() != ChangeOrderStatus.COMPLETED) {
            throw new IllegalStateException("CO must be COMPLETED to close. Current: " + co.getStatus());
        }
        co.setStatus(ChangeOrderStatus.CLOSED);
        co.setClosedAt(LocalDateTime.now());
        co.setUpdatedByUserId(userId);
        return changeOrderRepository.save(co);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Sums line item amounts and propagates to CO-level financial fields. */
    public void recalculateTotals(ChangeOrder co) {
        BigDecimal netExGst = co.getLineItems().stream()
                .map(ChangeOrderLineItem::getLineAmountExGst)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal gstAmount = netExGst.multiply(co.getGstRate())
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal netInclGst = netExGst.add(gstAmount)
                .setScale(6, RoundingMode.HALF_UP);

        co.setNetAmountExGst(netExGst);
        co.setGstAmount(gstAmount);
        co.setNetAmountInclGst(netInclGst);
    }

    private String generateReferenceNumber(Long projectId) {
        long count = changeOrderRepository.countByProjectIdAndDeletedAtIsNull(projectId);
        return String.format("CO-%d-%04d", projectId, count + 1);
    }
}
