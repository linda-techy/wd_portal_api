package com.wd.api.service;

import com.wd.api.dto.StagePaymentDtos.*;
import com.wd.api.event.StagePaymentCertifiedEvent;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.repository.PaymentStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles stage payment certification and retention:
 *
 *   certify → mark DUE → INVOICED → PAID
 *
 * On certification:
 *   1. Records certifiedBy + certifiedAt.
 *   2. Computes retention_held = stageAmountExGst * retentionPct (default 5%).
 *   3. Publishes StagePaymentCertifiedEvent so any VO progress tranches linked
 *      to this stage can be triggered.
 */
@Service
@Transactional
public class StagePaymentCertificationService {

    private static final Logger logger = LoggerFactory.getLogger(StagePaymentCertificationService.class);

    private final PaymentStageRepository stageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StagePaymentCertificationService(PaymentStageRepository stageRepository,
                                             ApplicationEventPublisher eventPublisher) {
        this.stageRepository  = stageRepository;
        this.eventPublisher   = eventPublisher;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PaymentStage> getProjectStages(Long projectId) {
        return stageRepository.findByProjectIdOrderByStageNumberAsc(projectId);
    }

    @Transactional(readOnly = true)
    public PaymentStage getStage(Long stageId) {
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
    }

    // -------------------------------------------------------------------------
    // Retention summary (used by FinalAccount flow)
    // -------------------------------------------------------------------------

    /**
     * Returns the total retention held across all payment stages for a project.
     * Used by the Final Account flow to know how much retention to release
     * after the defect liability period.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRetentionForProject(Long projectId) {
        return stageRepository.findByProjectIdOrderByStageNumberAsc(projectId).stream()
                .map(stage -> stage.getRetentionHeld() != null ? stage.getRetentionHeld() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -------------------------------------------------------------------------
    // Certify
    // -------------------------------------------------------------------------

    /**
     * Certify a payment stage. The stage must be UPCOMING or DUE.
     *
     * Sets certifiedBy, certifiedAt, computes retentionHeld, transitions to DUE
     * if still UPCOMING, then publishes StagePaymentCertifiedEvent.
     */
    public PaymentStage certify(Long stageId, CertifyStageRequest req, Long userId) {
        PaymentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));

        if (stage.getStatus() == PaymentStageStatus.INVOICED
                || stage.getStatus() == PaymentStageStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot certify a stage that is already " + stage.getStatus());
        }

        BigDecimal retentionPct = req.retentionPct() != null
                ? req.retentionPct()
                : new BigDecimal("0.0500");

        stage.setCertifiedBy(req.certifiedBy());
        stage.setCertifiedAt(LocalDateTime.now());
        stage.setRetentionPct(retentionPct);
        stage.recalculateNetPayable();   // computes retention + net payable
        stage.setUpdatedByUserId(userId);

        // Advance to DUE if still UPCOMING
        if (stage.getStatus() == PaymentStageStatus.UPCOMING) {
            stage.setStatus(PaymentStageStatus.DUE);
        }

        PaymentStage saved = stageRepository.save(stage);

        eventPublisher.publishEvent(new StagePaymentCertifiedEvent(
                this, saved.getId(),
                saved.getProject() != null ? saved.getProject().getId() : null,
                req.certifiedBy()));

        logger.info("Stage {} certified by {}. Retention held: {}",
                saved.getId(), req.certifiedBy(), saved.getRetentionHeld());

        return saved;
    }

    // -------------------------------------------------------------------------
    // Invoice / Payment recording (thin wrappers over existing PaymentStageService)
    // -------------------------------------------------------------------------

    /** Attach an invoice to a DUE stage and transition to INVOICED. */
    public PaymentStage attachInvoice(Long stageId, InvoiceStageRequest req, Long userId) {
        PaymentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));

        if (stage.getStatus() != PaymentStageStatus.DUE
                && stage.getStatus() != PaymentStageStatus.OVERDUE) {
            throw new IllegalStateException(
                    "Stage must be DUE or OVERDUE to attach an invoice. Current: " + stage.getStatus());
        }
        // Invoice entity is linked elsewhere; just update status
        stage.setStatus(PaymentStageStatus.INVOICED);
        stage.setUpdatedByUserId(userId);
        return stageRepository.save(stage);
    }

    /** Record a payment against an INVOICED stage; if fully paid → PAID. */
    public PaymentStage recordPayment(Long stageId, RecordStagePaymentRequest req, Long userId) {
        PaymentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));

        if (stage.getStatus() != PaymentStageStatus.INVOICED) {
            throw new IllegalStateException("Stage must be INVOICED to record payment.");
        }

        BigDecimal newTotal = stage.getPaidAmount().add(req.paidAmount());
        stage.setPaidAmount(newTotal);

        if (newTotal.compareTo(stage.getNetPayableAmount()) >= 0) {
            stage.setStatus(PaymentStageStatus.PAID);
            stage.setPaidAt(req.paidDate() != null
                    ? req.paidDate().atStartOfDay()
                    : LocalDateTime.now());
        }
        stage.setUpdatedByUserId(userId);
        return stageRepository.save(stage);
    }
}
