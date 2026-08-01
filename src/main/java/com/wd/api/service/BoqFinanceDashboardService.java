package com.wd.api.service;

import com.wd.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Aggregated finance dashboard for a project's BOQ / payment schedule.
 *
 * <p>All figures are computed from immutable stage amounts and recorded invoice
 * payments — never from live BOQ item data after the BOQ is approved.
 *
 * <p><strong>Single financial source (audit P1-3, owner decision 2026-05-25):</strong>
 * commercial change value is sourced <em>exclusively</em> from {@code change_orders}
 * (regular Change Orders and Variation Orders, the latter marked by {@code voCategory}).
 * {@code ProjectVariation} is a scheduling/status entity and is intentionally NOT a
 * financial vehicle — it must never be aggregated here. See
 * {@code BoqFinanceDashboardServiceTest} for the structural guard.
 */
@Service
@Transactional(readOnly = true)
public class BoqFinanceDashboardService {

    private final BoqDocumentRepository boqDocumentRepository;
    private final ChangeOrderRepository changeOrderRepository;
    private final BoqInvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final RefundNoticeRepository refundNoticeRepository;

    public BoqFinanceDashboardService(BoqDocumentRepository boqDocumentRepository,
                                       ChangeOrderRepository changeOrderRepository,
                                       BoqInvoiceRepository invoiceRepository,
                                       CreditNoteRepository creditNoteRepository,
                                       RefundNoticeRepository refundNoticeRepository) {
        this.boqDocumentRepository = boqDocumentRepository;
        this.changeOrderRepository = changeOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.refundNoticeRepository = refundNoticeRepository;
    }

    public ProjectFinanceSummary getSummary(Long projectId) {
        // Original contract value (from approved BOQ)
        BigDecimal originalContractValue = boqDocumentRepository
                .findApprovedByProjectId(projectId)
                .map(d -> d.getTotalValueInclGst())
                .orElse(BigDecimal.ZERO);

        // CO additions and reductions
        BigDecimal coAdditions = changeOrderRepository
                .findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId).stream()
                .filter(co -> co.isApproved() || co.getStatus().name().matches("IN_PROGRESS|COMPLETED|CLOSED"))
                .filter(co -> co.getCoType().isAddition())
                .map(co -> co.getNetAmountInclGst())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal coReductions = changeOrderRepository
                .findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId).stream()
                .filter(co -> co.isApproved() || co.getStatus().name().matches("IN_PROGRESS|COMPLETED|CLOSED"))
                .filter(co -> co.getCoType().isReduction())
                .map(co -> co.getNetAmountInclGst().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProjectValue = originalContractValue.add(coAdditions).subtract(coReductions);

        // Invoicing
        BigDecimal totalInvoiced = invoiceRepository
                .findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId).stream()
                .filter(inv -> !inv.isVoid())
                .map(inv -> inv.getTotalInclGst())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = invoiceRepository.sumTotalCollected(projectId);
        BigDecimal totalOutstanding = invoiceRepository.sumOutstandingStageInvoices(projectId);

        // Credits and refunds
        BigDecimal pendingCredits = creditNoteRepository.sumAvailableCredit(projectId);

        BigDecimal pendingRefunds = refundNoticeRepository
                .findByProjectIdOrderByIssuedAtDesc(projectId).stream()
                .filter(r -> r.getStatus().name().matches("PENDING|ACKNOWLEDGED|PROCESSING"))
                .map(r -> r.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProjectFinanceSummary(
                originalContractValue,
                coAdditions,
                coReductions,
                netProjectValue,
                totalInvoiced,
                totalCollected,
                totalOutstanding,
                pendingCredits,
                pendingRefunds
        );
    }

    // -------------------------------------------------------------------------
    // Summary record
    // -------------------------------------------------------------------------

    public record ProjectFinanceSummary(
            BigDecimal originalContractValue,
            BigDecimal coAdditions,
            BigDecimal coReductions,
            BigDecimal netProjectValue,
            BigDecimal totalInvoiced,
            BigDecimal totalCollected,
            BigDecimal totalOutstanding,
            BigDecimal pendingCredits,
            BigDecimal pendingRefundsToCustomer
    ) {}
}
