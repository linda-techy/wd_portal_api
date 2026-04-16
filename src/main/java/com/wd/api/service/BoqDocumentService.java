package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.repository.*;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.security.ProjectAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages the BOQ Document lifecycle and payment stage generation.
 *
 * Method 2 rules enforced:
 *   R-001  APPROVED is a terminal status — the BOQ is permanently locked.
 *   R-002  Payment stage amounts are computed ONCE at approval time and stored
 *          as immutable values. They are never recalculated from live data.
 *   R-003  All scope changes after approval must go through a ChangeOrder.
 */
@Service
@Transactional
public class BoqDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(BoqDocumentService.class);

    private final BoqDocumentRepository boqDocumentRepository;
    private final BoqItemRepository boqItemRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository portalUserRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final CustomerUserRepository customerUserRepository;
    private final ActivityFeedService activityFeedService;

    public BoqDocumentService(BoqDocumentRepository boqDocumentRepository,
                               BoqItemRepository boqItemRepository,
                               PaymentStageRepository paymentStageRepository,
                               CustomerProjectRepository projectRepository,
                               PortalUserRepository portalUserRepository,
                               ProjectAccessGuard projectAccessGuard,
                               CustomerUserRepository customerUserRepository,
                               ActivityFeedService activityFeedService) {
        this.boqDocumentRepository = boqDocumentRepository;
        this.boqItemRepository = boqItemRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.projectRepository = projectRepository;
        this.portalUserRepository = portalUserRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.customerUserRepository = customerUserRepository;
        this.activityFeedService = activityFeedService;
    }

    // -------------------------------------------------------------------------
    // BOQ Document queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BoqDocument getDocument(Long documentId) {
        return boqDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("BOQ document not found: " + documentId));
    }

    @Transactional(readOnly = true)
    public BoqDocument getDocument(Long documentId, Long userId) {
        BoqDocument doc = getDocument(documentId);
        projectAccessGuard.verifyPortalAccess(userId, doc.getProject().getId());
        return doc;
    }

    @Transactional(readOnly = true)
    public BoqDocument getApprovedDocument(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        return boqDocumentRepository.findApprovedByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException(
                    "No approved BOQ document found for project " + projectId));
    }

    @Transactional(readOnly = true)
    public List<BoqDocument> getProjectDocuments(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        return boqDocumentRepository.findActiveByProjectId(projectId);
    }

    // -------------------------------------------------------------------------
    // BOQ Document creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new DRAFT BOQ document for a project.
     * Only one non-APPROVED document may be active at a time; if a previous
     * document was REJECTED it gets a new revision number.
     */
    public BoqDocument createDocument(Long projectId, BigDecimal gstRate, Long userId) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        projectAccessGuard.verifyPortalAccess(userId, projectId);

        if (boqDocumentRepository.existsByProjectIdAndStatus(projectId, BoqDocumentStatus.APPROVED)) {
            throw new IllegalStateException(
                "Project " + projectId + " already has an approved BOQ. All scope changes must be submitted as Change Orders.");
        }

        int nextRevision = boqDocumentRepository.findActiveByProjectId(projectId).stream()
                .mapToInt(BoqDocument::getRevisionNumber)
                .max()
                .orElse(0) + 1;

        BoqDocument doc = new BoqDocument();
        doc.setProject(project);
        doc.setGstRate(gstRate != null ? gstRate : new BigDecimal("0.18"));
        doc.setRevisionNumber(nextRevision);
        doc.setCreatedByUserId(userId);
        return boqDocumentRepository.save(doc);
    }

    // -------------------------------------------------------------------------
    // Submission
    // -------------------------------------------------------------------------

    /**
     * Submits a DRAFT BOQ for customer approval.
     * Snapshots the current BOQ item totals into the document's financial fields.
     */
    public BoqDocument submitForApproval(Long documentId, Long userId) {
        BoqDocument doc = getDocument(documentId);

        projectAccessGuard.verifyPortalAccess(userId, doc.getProject().getId());

        if (!doc.isDraft()) {
            throw new IllegalStateException("Only a DRAFT BOQ document can be submitted. Current status: " + doc.getStatus());
        }

        // Snapshot totals from live boq_items
        BigDecimal totalExGst = boqItemRepository.findByProjectIdWithAssociations(doc.getProject().getId())
                .stream()
                .map(BoqItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal gstAmount = totalExGst.multiply(doc.getGstRate()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalInclGst = totalExGst.add(gstAmount).setScale(6, RoundingMode.HALF_UP);

        doc.setTotalValueExGst(totalExGst);
        doc.setTotalGstAmount(gstAmount);
        doc.setTotalValueInclGst(totalInclGst);
        doc.setStatus(BoqDocumentStatus.PENDING_APPROVAL);
        doc.setSubmittedAt(LocalDateTime.now());

        PortalUser submitter = portalUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        doc.setSubmittedBy(submitter);
        doc.setUpdatedByUserId(userId);

        BoqDocument saved = boqDocumentRepository.save(doc);

        activityFeedService.logProjectActivity(
            "BOQ_SUBMITTED", "BOQ Submitted for Approval",
            "BOQ revision " + doc.getRevisionNumber() + " submitted for customer approval.",
            doc.getProject(), getCurrentPortalUser());

        return saved;
    }

    // -------------------------------------------------------------------------
    // Portal-side internal approval
    // -------------------------------------------------------------------------

    public BoqDocument approveInternally(Long documentId, Long userId) {
        BoqDocument doc = getDocument(documentId);

        projectAccessGuard.verifyPortalAccess(userId, doc.getProject().getId());

        if (!doc.isPendingApproval()) {
            throw new IllegalStateException("BOQ must be PENDING_APPROVAL to approve internally. Current: " + doc.getStatus());
        }

        PortalUser approver = portalUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        doc.setApprovedBy(approver);
        doc.setApprovedAt(LocalDateTime.now());
        doc.setUpdatedByUserId(userId);
        // Stay in PENDING_APPROVAL — moves to APPROVED only after customer confirms
        return boqDocumentRepository.save(doc);
    }

    // -------------------------------------------------------------------------
    // Customer approval — triggers payment stage generation (R-001 + R-002)
    // -------------------------------------------------------------------------

    /**
     * Records customer approval and locks the BOQ permanently.
     * Auto-generates PaymentStage records with immutable frozen amounts.
     *
     * @param customerSignedById  the customer user who signed the BOQ (required)
     * @param stageConfigs        list of (stageName, percentage) pairs summing to 1.0
     */
    public BoqDocument recordCustomerApproval(Long documentId,
                                               Long portalUserId,
                                               Long customerSignedById,
                                               List<StageConfig> stageConfigs) {
        BoqDocument doc = getDocument(documentId);

        projectAccessGuard.verifyPortalAccess(portalUserId, doc.getProject().getId());

        if (doc.getStatus() != BoqDocumentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "BOQ must be PENDING_APPROVAL for customer approval. Current: " + doc.getStatus());
        }

        // Validate customerSignedById
        if (customerSignedById == null) {
            throw new IllegalArgumentException("customerSignedById is required");
        }
        customerUserRepository.findById(customerSignedById)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Customer user not found: " + customerSignedById));
        // Verify they are a member of the project
        projectAccessGuard.verifyCustomerMembership(customerSignedById, doc.getProject().getId());

        validateStagePercentages(stageConfigs);

        doc.setStatus(BoqDocumentStatus.APPROVED);
        doc.setCustomerApprovedAt(LocalDateTime.now());
        doc.setCustomerApprovedBy(customerSignedById);   // now always non-null
        BoqDocument saved = boqDocumentRepository.save(doc);

        generatePaymentStages(saved, stageConfigs);

        activityFeedService.logProjectActivity(
            "BOQ_APPROVED", "BOQ Approved",
            "BOQ revision " + doc.getRevisionNumber() + " approved by customer.",
            doc.getProject(), null);  // null portalUser because customer triggered this

        logger.info("BOQ document {} approved for project {}. Signed by customer user {}. {} payment stages generated.",
                documentId, doc.getProject().getId(), customerSignedById, stageConfigs.size());

        return saved;
    }

    // -------------------------------------------------------------------------
    // Rejection
    // -------------------------------------------------------------------------

    public BoqDocument reject(Long documentId, Long rejectorId, String reason) {
        BoqDocument doc = getDocument(documentId);

        projectAccessGuard.verifyPortalAccess(rejectorId, doc.getProject().getId());

        if (!doc.isPendingApproval()) {
            throw new IllegalStateException("Only PENDING_APPROVAL BOQ can be rejected. Current: " + doc.getStatus());
        }

        doc.setStatus(BoqDocumentStatus.REJECTED);
        doc.setRejectedAt(LocalDateTime.now());
        doc.setRejectedBy(rejectorId);
        doc.setRejectionReason(reason);
        doc.setUpdatedByUserId(rejectorId);
        BoqDocument saved = boqDocumentRepository.save(doc);

        activityFeedService.logProjectActivity(
            "BOQ_REJECTED", "BOQ Rejected",
            "BOQ revision " + doc.getRevisionNumber() + " rejected. Reason: " + doc.getRejectionReason(),
            doc.getProject(), getCurrentPortalUser());

        return saved;
    }

    // -------------------------------------------------------------------------
    // Payment stage generation (R-002 — called only once at approval)
    // -------------------------------------------------------------------------

    private void generatePaymentStages(BoqDocument doc, List<StageConfig> configs) {
        BigDecimal boqSnapshot = doc.getTotalValueExGst();
        BigDecimal gstRate = doc.getGstRate();
        Long projectId = doc.getProject().getId();

        for (int i = 0; i < configs.size(); i++) {
            StageConfig cfg = configs.get(i);

            BigDecimal stageExGst = boqSnapshot.multiply(cfg.percentage())
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal stageGst = stageExGst.multiply(gstRate)
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal stageInclGst = stageExGst.add(stageGst)
                    .setScale(6, RoundingMode.HALF_UP);

            PaymentStage stage = new PaymentStage();
            stage.setBoqDocument(doc);
            stage.setProject(doc.getProject());
            stage.setStageNumber(i + 1);
            stage.setStageName(cfg.name());
            stage.setBoqValueSnapshot(boqSnapshot);          // R-002: frozen snapshot
            stage.setStagePercentage(cfg.percentage());
            stage.setStageAmountExGst(stageExGst);
            stage.setGstRate(gstRate);
            stage.setGstAmount(stageGst);
            stage.setStageAmountInclGst(stageInclGst);
            stage.setNetPayableAmount(stageInclGst);         // starts equal to gross amount
            stage.setStatus(PaymentStageStatus.UPCOMING);
            stage.setCreatedByUserId(null);

            paymentStageRepository.save(stage);
        }
    }

    private void validateStagePercentages(List<StageConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("At least one payment stage must be defined.");
        }
        BigDecimal total = configs.stream()
                .map(StageConfig::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Allow ±0.001 tolerance for rounding
        if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.001")) > 0) {
            throw new IllegalArgumentException(
                "Stage percentages must sum to 1.0 (100%). Current total: " + total);
        }
    }

    // -------------------------------------------------------------------------
    // Inner record for stage configuration
    // -------------------------------------------------------------------------

    public record StageConfig(String name, BigDecimal percentage) {}

    private PortalUser getCurrentPortalUser() {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
            return portalUserRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;  // system-triggered actions have no portal user
        }
    }
}
