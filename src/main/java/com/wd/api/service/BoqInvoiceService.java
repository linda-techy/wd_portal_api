package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.model.enums.ProjectInvoiceStatus;
import com.wd.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages BOQ Invoice creation and lifecycle.
 *
 * Two invoice lanes (Method 2):
 *   STAGE_INVOICE — one per PaymentStage; stage amount + GST + credit note lines only.
 *   CO_INVOICE    — one per CO billing event; ADVANCE / MIDPOINT / BALANCE.
 *
 * Stage invoice rule: never include CO amounts. Never recalculate stage amounts.
 */
@Service
@Transactional
public class BoqInvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(BoqInvoiceService.class);

    private final BoqInvoiceRepository invoiceRepository;
    private final PaymentStageRepository stageRepository;
    private final CustomerProjectRepository projectRepository;

    public BoqInvoiceService(BoqInvoiceRepository invoiceRepository,
                              PaymentStageRepository stageRepository,
                              CustomerProjectRepository projectRepository) {
        this.invoiceRepository = invoiceRepository;
        this.stageRepository = stageRepository;
        this.projectRepository = projectRepository;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BoqInvoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
    }

    @Transactional(readOnly = true)
    public List<BoqInvoice> getProjectInvoices(Long projectId) {
        return invoiceRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public List<BoqInvoice> getStageInvoices(Long projectId) {
        return invoiceRepository.findByProjectIdAndInvoiceTypeAndDeletedAtIsNull(projectId, "STAGE_INVOICE");
    }

    @Transactional(readOnly = true)
    public List<BoqInvoice> getCoInvoices(Long projectId) {
        return invoiceRepository.findByProjectIdAndInvoiceTypeAndDeletedAtIsNull(projectId, "CO_INVOICE");
    }

    // -------------------------------------------------------------------------
    // Stage Invoice
    // -------------------------------------------------------------------------

    /**
     * Raises a STAGE_INVOICE for the given PaymentStage.
     * Contains only the stage amount + GST + any pre-applied credit note deductions.
     * Net amount = stage_amount_incl_gst - applied_credit_amount (already on stage).
     */
    public BoqInvoice raiseStageInvoice(Long stageId, LocalDate dueDate, Long userId) {
        PaymentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Payment stage not found: " + stageId));

        if (stage.getStatus() == PaymentStageStatus.INVOICED || stage.getStatus() == PaymentStageStatus.PAID) {
            throw new IllegalStateException("Stage " + stageId + " has already been invoiced.");
        }

        String invoiceNumber = generateInvoiceNumber(stage.getProject().getId());

        BoqInvoice invoice = new BoqInvoice();
        invoice.setProject(stage.getProject());
        invoice.setInvoiceType("STAGE_INVOICE");
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPaymentStage(stage);
        invoice.setGstRate(stage.getGstRate());
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(dueDate);
        invoice.setCreatedByUserId(userId);

        // Ensure retention & net payable are up-to-date before reading amounts
        stage.recalculateNetPayable();

        // Stage amount (immutable — read directly from frozen fields)
        BigDecimal stageAmountExGst = stage.getStageAmountExGst();
        BigDecimal gstAmount = stage.getGstAmount();
        BigDecimal grossInclGst = stage.getStageAmountInclGst();
        BigDecimal retentionHeld = stage.getRetentionHeld() != null ? stage.getRetentionHeld() : BigDecimal.ZERO;
        BigDecimal creditApplied = stage.getAppliedCreditAmount();
        BigDecimal netDue = stage.getNetPayableAmount();

        invoice.setSubtotalExGst(stageAmountExGst);
        invoice.setGstAmount(gstAmount);
        invoice.setTotalInclGst(grossInclGst);
        invoice.setTotalCreditApplied(creditApplied);
        invoice.setNetAmountDue(netDue);

        // Build line items
        int sortIdx = 0;
        List<BoqInvoiceLineItem> lines = new ArrayList<>();
        lines.add(lineItem(invoice, "STAGE_AMOUNT", stage.getStageName() + " — stage " + stage.getStageNumber(),
                BigDecimal.ONE, stageAmountExGst, ++sortIdx));
        lines.add(lineItem(invoice, "GST",
                "GST @ " + stage.getGstRate().multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%",
                BigDecimal.ONE, gstAmount, ++sortIdx));
        if (retentionHeld.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(lineItem(invoice, "RETENTION_DEDUCTION",
                    "Retention @ " + stage.getRetentionPct().multiply(new BigDecimal("100"))
                            .stripTrailingZeros().toPlainString() + "%",
                    BigDecimal.ONE, retentionHeld.negate(), ++sortIdx));
        }
        if (creditApplied.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(lineItem(invoice, "CREDIT_NOTE_DEDUCTION",
                    "Credit note deduction", BigDecimal.ONE, creditApplied.negate(), ++sortIdx));
        }
        invoice.setLineItems(lines);

        BoqInvoice saved = invoiceRepository.save(invoice);

        // Advance stage status to INVOICED
        stage.setStatus(PaymentStageStatus.INVOICED);
        stage.setInvoice(saved);
        stageRepository.save(stage);

        logger.info("Stage invoice {} raised for stage {} project {}",
                invoiceNumber, stageId, stage.getProject().getId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // CO Invoice — auto-generated on addition CO approval (ADVANCE event)
    // -------------------------------------------------------------------------

    public BoqInvoice generateCoAdvanceInvoice(ChangeOrder co) {
        String invoiceNumber = generateInvoiceNumber(co.getProject().getId());

        BoqInvoice invoice = new BoqInvoice();
        invoice.setProject(co.getProject());
        invoice.setInvoiceType("CO_INVOICE");
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setChangeOrder(co);
        invoice.setCoBillingEvent("ADVANCE");
        invoice.setGstRate(co.getGstRate());
        invoice.setIssueDate(LocalDate.now());

        // Standard 30% advance on CO approval
        BigDecimal advanceRatio = new BigDecimal("0.30");
        BigDecimal advanceExGst = co.getNetAmountExGst().multiply(advanceRatio).setScale(6, RoundingMode.HALF_UP);
        BigDecimal advanceGst = advanceExGst.multiply(co.getGstRate()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal advanceInclGst = advanceExGst.add(advanceGst).setScale(6, RoundingMode.HALF_UP);

        invoice.setSubtotalExGst(advanceExGst);
        invoice.setGstAmount(advanceGst);
        invoice.setTotalInclGst(advanceInclGst);
        invoice.setNetAmountDue(advanceInclGst);

        List<BoqInvoiceLineItem> lines = new ArrayList<>();
        lines.add(lineItem(invoice, "CO_AMOUNT",
                "CO Advance (30%) — " + co.getReferenceNumber() + ": " + co.getTitle(),
                BigDecimal.ONE, advanceExGst, 1));
        lines.add(lineItem(invoice, "GST",
                "GST @ " + co.getGstRate().multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%",
                BigDecimal.ONE, advanceGst, 2));
        invoice.setLineItems(lines);

        BoqInvoice saved = invoiceRepository.save(invoice);
        logger.info("CO advance invoice {} generated for CO {} project {}",
                invoiceNumber, co.getReferenceNumber(), co.getProject().getId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Send / mark viewed / confirm payment
    // -------------------------------------------------------------------------

    public BoqInvoice send(Long invoiceId, Long userId) {
        BoqInvoice invoice = getInvoice(invoiceId);
        if (!invoice.isDraft()) {
            throw new IllegalStateException("Only DRAFT invoices can be sent. Current: " + invoice.getStatus());
        }
        invoice.setStatus(ProjectInvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        invoice.setUpdatedByUserId(userId);
        return invoiceRepository.save(invoice);
    }

    public BoqInvoice markViewed(Long invoiceId) {
        BoqInvoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == ProjectInvoiceStatus.SENT) {
            invoice.setStatus(ProjectInvoiceStatus.VIEWED);
            invoice.setViewedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
        }
        return invoice;
    }

    public BoqInvoice confirmPayment(Long invoiceId, String paymentReference,
                                      String paymentMethod, Long userId) {
        BoqInvoice invoice = getInvoice(invoiceId);

        if (invoice.isPaid() || invoice.isVoid()) {
            throw new IllegalStateException("Invoice " + invoiceId + " is already " + invoice.getStatus());
        }

        invoice.setStatus(ProjectInvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setPaymentReference(paymentReference);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setUpdatedByUserId(userId);

        BoqInvoice saved = invoiceRepository.save(invoice);

        // Advance associated stage to PAID
        if (invoice.isStageInvoice() && invoice.getPaymentStage() != null) {
            PaymentStage stage = invoice.getPaymentStage();
            stage.setStatus(PaymentStageStatus.PAID);
            stage.setPaidAmount(invoice.getNetAmountDue());
            stage.setPaidAt(LocalDateTime.now());
            stageRepository.save(stage);
        }

        return saved;
    }

    public BoqInvoice dispute(Long invoiceId, String reason, Long userId) {
        BoqInvoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() != ProjectInvoiceStatus.SENT
                && invoice.getStatus() != ProjectInvoiceStatus.VIEWED
                && invoice.getStatus() != ProjectInvoiceStatus.OVERDUE) {
            throw new IllegalStateException("Invoice cannot be disputed in status: " + invoice.getStatus());
        }
        invoice.setStatus(ProjectInvoiceStatus.DISPUTED);
        invoice.setDisputedAt(LocalDateTime.now());
        invoice.setDisputeReason(reason);
        invoice.setUpdatedByUserId(userId);
        return invoiceRepository.save(invoice);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private BoqInvoiceLineItem lineItem(BoqInvoice invoice, String lineType,
                                         String description, BigDecimal qty,
                                         BigDecimal unitPrice, int sortOrder) {
        BoqInvoiceLineItem li = new BoqInvoiceLineItem();
        li.setInvoice(invoice);
        li.setLineType(lineType);
        li.setDescription(description);
        li.setQuantity(qty);
        li.setUnitPrice(unitPrice);
        li.setAmount(qty.multiply(unitPrice).setScale(6, RoundingMode.HALF_UP));
        li.setSortOrder(sortOrder);
        return li;
    }

    private String generateInvoiceNumber(Long projectId) {
        long count = invoiceRepository.countByProjectIdAndDeletedAtIsNull(projectId);
        return String.format("INV-%d-%04d", projectId, count + 1);
    }
}
