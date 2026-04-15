package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.model.enums.CreditNoteStatus;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.model.enums.RefundNoticeStatus;
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
 * Auto-generates Credit Notes on reduction CO approval and applies them
 * sequentially to future stage invoices.
 *
 * If credit exceeds all remaining stage invoices a RefundNotice is raised.
 */
@Service
@Transactional
public class CreditNoteService {

    private static final Logger logger = LoggerFactory.getLogger(CreditNoteService.class);

    private final CreditNoteRepository creditNoteRepository;
    private final RefundNoticeRepository refundNoticeRepository;
    private final PaymentStageRepository stageRepository;
    private final BoqInvoiceRepository invoiceRepository;

    public CreditNoteService(CreditNoteRepository creditNoteRepository,
                              RefundNoticeRepository refundNoticeRepository,
                              PaymentStageRepository stageRepository,
                              BoqInvoiceRepository invoiceRepository) {
        this.creditNoteRepository = creditNoteRepository;
        this.refundNoticeRepository = refundNoticeRepository;
        this.stageRepository = stageRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // -------------------------------------------------------------------------
    // Auto-generation on reduction CO approval
    // -------------------------------------------------------------------------

    /**
     * Called by ChangeOrderService when a reduction CO is approved.
     * Creates a CreditNote and immediately attempts to apply it to upcoming stages.
     * If credit > remaining stage totals, also raises a RefundNotice.
     */
    public CreditNote generateForReduction(ChangeOrder co) {
        BigDecimal absAmountExGst = co.getNetAmountExGst().abs().setScale(6, RoundingMode.HALF_UP);
        BigDecimal gstAmount = absAmountExGst.multiply(co.getGstRate()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalCredit = absAmountExGst.add(gstAmount).setScale(6, RoundingMode.HALF_UP);

        String creditNoteNumber = generateCreditNoteNumber(co.getProject().getId());

        CreditNote cn = new CreditNote();
        cn.setProject(co.getProject());
        cn.setChangeOrder(co);
        cn.setCreditNoteNumber(creditNoteNumber);
        cn.setCreditAmountExGst(absAmountExGst);
        cn.setGstRate(co.getGstRate());
        cn.setGstAmount(gstAmount);
        cn.setTotalCreditInclGst(totalCredit);
        cn.setRemainingBalance(totalCredit);
        cn.setStatus(CreditNoteStatus.AVAILABLE);

        CreditNote saved = creditNoteRepository.save(cn);
        logger.info("Credit note {} issued for CO {} — amount: {}",
                creditNoteNumber, co.getReferenceNumber(), totalCredit);

        // Apply credit sequentially to upcoming/due stages
        applyCreditToStages(saved);

        return saved;
    }

    // -------------------------------------------------------------------------
    // Sequential application to stage invoices
    // -------------------------------------------------------------------------

    /**
     * Applies a credit note's remaining balance to UPCOMING/DUE stages in order.
     * For each eligible stage: deduct as much as possible from its net_payable_amount.
     * If balance runs out before all stages are covered, the credit is partially applied.
     * If there are no more stages but credit remains, a RefundNotice is raised.
     */
    public void applyCreditToStages(CreditNote creditNote) {
        if (creditNote.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) return;

        List<PaymentStage> eligible = stageRepository
                .findEligibleForCreditApplication(creditNote.getProject().getId());

        for (PaymentStage stage : eligible) {
            if (creditNote.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal stageNet = stage.getNetPayableAmount();
            BigDecimal toApply = creditNote.getRemainingBalance().min(stageNet);

            // Apply credit to this stage
            stage.setAppliedCreditAmount(
                    stage.getAppliedCreditAmount().add(toApply).setScale(6, RoundingMode.HALF_UP));
            stage.recalculateNetPayable();
            if (stage.getNetPayableAmount().compareTo(BigDecimal.ZERO) == 0) {
                stage.setStatus(PaymentStageStatus.ON_HOLD);
            }
            stageRepository.save(stage);

            // Record application
            CreditNoteApplication app = new CreditNoteApplication();
            app.setCreditNote(creditNote);
            app.setAppliedAmount(toApply);
            app.setAppliedAt(LocalDateTime.now());
            // Link to stage's invoice if it already exists; otherwise leave null
            if (stage.getInvoice() != null) {
                app.setInvoice(stage.getInvoice());
            }
            creditNote.getApplications().add(app);

            // Reduce remaining balance
            creditNote.setRemainingBalance(
                    creditNote.getRemainingBalance().subtract(toApply).setScale(6, RoundingMode.HALF_UP));

            logger.debug("Applied {} credit to stage {} (remaining balance: {})",
                    toApply, stage.getId(), creditNote.getRemainingBalance());
        }

        // Update credit note status
        if (creditNote.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0) {
            creditNote.setStatus(CreditNoteStatus.FULLY_APPLIED);
            creditNote.setFullyAppliedAt(LocalDateTime.now());
        } else if (creditNote.getRemainingBalance().compareTo(creditNote.getTotalCreditInclGst()) < 0) {
            creditNote.setStatus(CreditNoteStatus.PARTIALLY_APPLIED);
        }
        creditNoteRepository.save(creditNote);

        // If credit still remains after exhausting all stages → RefundNotice
        if (creditNote.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
            raiseRefundNotice(creditNote);
        }
    }

    // -------------------------------------------------------------------------
    // Refund Notice
    // -------------------------------------------------------------------------

    private void raiseRefundNotice(CreditNote creditNote) {
        String refNumber = generateRefundNumber(creditNote.getProject().getId());

        RefundNotice refund = new RefundNotice();
        refund.setProject(creditNote.getProject());
        refund.setCreditNote(creditNote);
        refund.setReferenceNumber(refNumber);
        refund.setRefundAmount(creditNote.getRemainingBalance());
        refund.setReason("Excess credit from CO reduction " + creditNote.getCreditNoteNumber()
                + " exceeds all remaining stage invoices.");
        refund.setStatus(RefundNoticeStatus.PENDING);

        refundNoticeRepository.save(refund);
        logger.warn("Refund notice {} raised — excess credit {} on project {}",
                refNumber, creditNote.getRemainingBalance(), creditNote.getProject().getId());
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CreditNote> getProjectCreditNotes(Long projectId) {
        return creditNoteRepository.findByProjectIdOrderByIssuedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<RefundNotice> getProjectRefundNotices(Long projectId) {
        return refundNoticeRepository.findByProjectIdOrderByIssuedAtDesc(projectId);
    }

    public RefundNotice acknowledgeRefund(Long refundId, Long userId) {
        RefundNotice refund = refundNoticeRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund notice not found: " + refundId));
        if (refund.getStatus() != RefundNoticeStatus.PENDING) {
            throw new IllegalStateException("Refund notice is not PENDING. Current: " + refund.getStatus());
        }
        refund.setStatus(RefundNoticeStatus.ACKNOWLEDGED);
        refund.setAcknowledgedAt(LocalDateTime.now());
        refund.setUpdatedByUserId(userId);
        return refundNoticeRepository.save(refund);
    }

    public RefundNotice completeRefund(Long refundId, String paymentReference, Long userId) {
        RefundNotice refund = refundNoticeRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund notice not found: " + refundId));
        refund.setStatus(RefundNoticeStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setPaymentReference(paymentReference);
        refund.setUpdatedByUserId(userId);
        return refundNoticeRepository.save(refund);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String generateCreditNoteNumber(Long projectId) {
        long count = creditNoteRepository.countByProjectId(projectId);
        return String.format("CN-%d-%04d", projectId, count + 1);
    }

    private String generateRefundNumber(Long projectId) {
        long count = refundNoticeRepository.countByProjectId(projectId);
        return String.format("RFD-%d-%04d", projectId, count + 1);
    }
}
