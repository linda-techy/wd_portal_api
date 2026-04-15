package com.wd.api.service;

import com.wd.api.dto.DeductionRegisterDtos.*;
import com.wd.api.model.ChangeOrder;
import com.wd.api.model.DeductionRegister;
import com.wd.api.model.enums.DeductionDecision;
import com.wd.api.model.enums.EscalationStatus;
import com.wd.api.repository.ChangeOrderRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DeductionRegisterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Manages the Deduction Register lifecycle:
 *
 *   create → PENDING
 *   decide → ACCEPTABLE / PARTIALLY_ACCEPTABLE / REJECTED
 *   escalate → ESCALATED → RESOLVED
 *   settle  → settled_in_final_account = true (called by FinalAccountService)
 *
 * Business rules:
 * - PARTIALLY_ACCEPTABLE requires acceptedAmount ≤ requestedAmount.
 * - REJECTED requires rejectionReason.
 * - A settled deduction cannot be re-decided.
 */
@Service
@Transactional
public class DeductionRegisterService {

    private static final Logger logger = LoggerFactory.getLogger(DeductionRegisterService.class);

    private final DeductionRegisterRepository deductionRepository;
    private final CustomerProjectRepository projectRepository;
    private final ChangeOrderRepository changeOrderRepository;

    public DeductionRegisterService(DeductionRegisterRepository deductionRepository,
                                     CustomerProjectRepository projectRepository,
                                     ChangeOrderRepository changeOrderRepository) {
        this.deductionRepository  = deductionRepository;
        this.projectRepository    = projectRepository;
        this.changeOrderRepository = changeOrderRepository;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DeductionRegister> getByProject(Long projectId) {
        return deductionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public DeductionRegister getById(Long id) {
        return deductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deduction not found: " + id));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalAcceptedAmount(Long projectId) {
        return deductionRepository.sumAcceptedAmountsByProjectId(projectId);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public DeductionRegister create(Long projectId, CreateDeductionRequest req) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        DeductionRegister dr = new DeductionRegister();
        dr.setProject(project);
        dr.setItemDescription(req.itemDescription());
        dr.setRequestedAmount(req.requestedAmount());
        dr.setDecision(DeductionDecision.PENDING);
        dr.setEscalationStatus(EscalationStatus.NONE);

        if (req.coId() != null) {
            ChangeOrder co = changeOrderRepository.findById(req.coId())
                    .orElseThrow(() -> new IllegalArgumentException("CO not found: " + req.coId()));
            dr.setChangeOrder(co);
        }

        DeductionRegister saved = deductionRepository.save(dr);
        logger.info("Deduction created: id={} project={} amount={}", saved.getId(), projectId, req.requestedAmount());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Decision
    // -------------------------------------------------------------------------

    public DeductionRegister recordDecision(Long deductionId, DeductionDecisionRequest req) {
        DeductionRegister dr = requirePendingOrEscalated(deductionId);

        if (dr.isSettledInFinalAccount()) {
            throw new IllegalStateException("Cannot re-decide a deduction already settled in the final account.");
        }

        validateDecisionRequest(req);

        dr.setDecision(req.decision());
        dr.setApprovedBy(req.approvedBy());
        dr.setDecisionDate(req.decisionDate() != null ? req.decisionDate() : LocalDate.now());

        switch (req.decision()) {
            case ACCEPTABLE -> dr.setAcceptedAmount(dr.getRequestedAmount());
            case PARTIALLY_ACCEPTABLE -> {
                if (req.acceptedAmount() == null) {
                    throw new IllegalArgumentException("acceptedAmount is required for PARTIALLY_ACCEPTABLE.");
                }
                if (req.acceptedAmount().compareTo(dr.getRequestedAmount()) > 0) {
                    throw new IllegalArgumentException(
                            "acceptedAmount cannot exceed requestedAmount.");
                }
                dr.setAcceptedAmount(req.acceptedAmount());
            }
            case REJECTED -> {
                dr.setRejectionReason(req.rejectionReason());
                dr.setAcceptedAmount(BigDecimal.ZERO);
            }
            default -> throw new IllegalArgumentException("Invalid decision: " + req.decision());
        }

        // If it was escalated and now decided, resolve the escalation
        if (dr.getEscalationStatus() == EscalationStatus.ESCALATED) {
            dr.setEscalationStatus(EscalationStatus.RESOLVED);
        }

        logger.info("Deduction {} decided: {} accepted={}",
                deductionId, req.decision(), dr.getAcceptedAmount());
        return deductionRepository.save(dr);
    }

    // -------------------------------------------------------------------------
    // Escalation
    // -------------------------------------------------------------------------

    public DeductionRegister escalate(Long deductionId, EscalateDeductionRequest req) {
        DeductionRegister dr = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new IllegalArgumentException("Deduction not found: " + deductionId));

        if (dr.getDecision() != DeductionDecision.PENDING) {
            throw new IllegalStateException(
                    "Can only escalate a PENDING deduction. Current decision: " + dr.getDecision());
        }
        if (dr.getEscalationStatus() == EscalationStatus.ESCALATED) {
            throw new IllegalStateException("Deduction is already escalated.");
        }

        dr.setEscalationStatus(EscalationStatus.ESCALATED);
        dr.setEscalatedTo(req.escalatedTo());
        logger.info("Deduction {} escalated to {}", deductionId, req.escalatedTo());
        return deductionRepository.save(dr);
    }

    // -------------------------------------------------------------------------
    // Settlement (called by FinalAccountService)
    // -------------------------------------------------------------------------

    /** Marks all unsettled deductions for a project as settled in the final account. */
    public int settleAllForProject(Long projectId) {
        List<DeductionRegister> unsettled =
                deductionRepository.findByProjectIdAndSettledInFinalAccountFalse(projectId);
        for (DeductionRegister dr : unsettled) {
            dr.setSettledInFinalAccount(true);
            deductionRepository.save(dr);
        }
        logger.info("Settled {} deductions for project {}", unsettled.size(), projectId);
        return unsettled.size();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DeductionRegister requirePendingOrEscalated(Long id) {
        DeductionRegister dr = deductionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deduction not found: " + id));
        if (dr.getDecision() != DeductionDecision.PENDING) {
            throw new IllegalStateException(
                    "Deduction already decided: " + dr.getDecision());
        }
        return dr;
    }

    private void validateDecisionRequest(DeductionDecisionRequest req) {
        if (req.decision() == null) {
            throw new IllegalArgumentException("Decision is required.");
        }
        if (req.decision() == DeductionDecision.REJECTED
                && (req.rejectionReason() == null || req.rejectionReason().isBlank())) {
            throw new IllegalArgumentException("rejectionReason is required when rejecting a deduction.");
        }
    }
}
