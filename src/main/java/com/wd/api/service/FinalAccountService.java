package com.wd.api.service;

import com.wd.api.dto.FinalAccountDtos.*;
import com.wd.api.event.FinalAccountAgreedEvent;
import com.wd.api.model.FinalAccount;
import com.wd.api.model.enums.FinalAccountStatus;
import com.wd.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Manages the Final Account lifecycle:
 *
 *   DRAFT → SUBMITTED → DISPUTED → AGREED → CLOSED
 *
 * On AGREED:
 *   - Publishes FinalAccountAgreedEvent (triggers completion tranche invoicing,
 *     deduction settlement, DLP start).
 *
 * Business rules:
 *   R-FA-1: One final account per project (enforced by UNIQUE DB constraint).
 *   R-FA-2: Financial totals can only be edited in DRAFT state.
 *   R-FA-3: Retention can only be released after AGREED + DLP end date passed.
 *   R-FA-4: DISPUTED → can go back to SUBMITTED for renegotiation.
 *   R-FA-5: CLOSED is terminal.
 */
@Service
@Transactional
public class FinalAccountService {

    private static final Logger logger = LoggerFactory.getLogger(FinalAccountService.class);

    private final FinalAccountRepository finalAccountRepository;
    private final CustomerProjectRepository projectRepository;
    private final DeductionRegisterRepository deductionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FinalAccountService(FinalAccountRepository finalAccountRepository,
                                CustomerProjectRepository projectRepository,
                                DeductionRegisterRepository deductionRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.finalAccountRepository = finalAccountRepository;
        this.projectRepository      = projectRepository;
        this.deductionRepository    = deductionRepository;
        this.eventPublisher         = eventPublisher;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public FinalAccount getByProject(Long projectId) {
        return finalAccountRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No final account found for project: " + projectId));
    }

    // -------------------------------------------------------------------------
    // Create (DRAFT)
    // -------------------------------------------------------------------------

    public FinalAccount create(Long projectId, CreateFinalAccountRequest req) {
        if (finalAccountRepository.existsByProjectId(projectId)) {
            throw new IllegalStateException(
                    "A final account already exists for project " + projectId);
        }

        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        FinalAccount fa = new FinalAccount();
        fa.setProject(project);
        fa.setBaseContractValue(req.baseContractValue());
        fa.setTotalAdditions(orZero(req.totalAdditions()));
        fa.setTotalAcceptedDeductions(orZero(req.totalAcceptedDeductions()));
        fa.setTotalReceivedToDate(orZero(req.totalReceivedToDate()));
        fa.setTotalRetentionHeld(orZero(req.totalRetentionHeld()));
        fa.setDlpStartDate(req.dlpStartDate());
        fa.setDlpEndDate(req.dlpEndDate());
        fa.setPreparedBy(req.preparedBy());
        fa.setStatus(FinalAccountStatus.DRAFT);

        FinalAccount saved = finalAccountRepository.save(fa);
        logger.info("Final account created for project {} by {}", projectId, req.preparedBy());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Update (DRAFT only)
    // -------------------------------------------------------------------------

    public FinalAccount update(Long projectId, UpdateFinalAccountRequest req) {
        FinalAccount fa = requireStatus(projectId, FinalAccountStatus.DRAFT);

        if (req.baseContractValue() != null)       fa.setBaseContractValue(req.baseContractValue());
        if (req.totalAdditions() != null)           fa.setTotalAdditions(req.totalAdditions());
        if (req.totalAcceptedDeductions() != null)  fa.setTotalAcceptedDeductions(req.totalAcceptedDeductions());
        if (req.totalReceivedToDate() != null)      fa.setTotalReceivedToDate(req.totalReceivedToDate());
        if (req.totalRetentionHeld() != null)       fa.setTotalRetentionHeld(req.totalRetentionHeld());
        if (req.dlpStartDate() != null)             fa.setDlpStartDate(req.dlpStartDate());
        if (req.dlpEndDate() != null)               fa.setDlpEndDate(req.dlpEndDate());
        if (req.preparedBy() != null)               fa.setPreparedBy(req.preparedBy());

        return finalAccountRepository.save(fa);
    }

    /**
     * Recompute financial totals from live data (accepted deductions sum, paid stage amounts).
     * Convenience method Finance team can call before submitting.
     */
    public FinalAccount recomputeTotals(Long projectId) {
        FinalAccount fa = requireStatus(projectId, FinalAccountStatus.DRAFT);

        BigDecimal acceptedDeductions = deductionRepository.sumAcceptedAmountsByProjectId(projectId);
        fa.setTotalAcceptedDeductions(acceptedDeductions);

        return finalAccountRepository.save(fa);
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    public FinalAccount transitionStatus(Long projectId, FinalAccountStatusRequest req) {
        FinalAccount fa = finalAccountRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No final account for project: " + projectId));

        validateTransition(fa.getStatus(), req.targetStatus());

        fa.setStatus(req.targetStatus());

        if (req.targetStatus() == FinalAccountStatus.AGREED) {
            if (req.agreedBy() == null || req.agreedBy().isBlank()) {
                throw new IllegalArgumentException("agreedBy is required when moving to AGREED.");
            }
            fa.setAgreedBy(req.agreedBy());
            // Begin DLP if not already set
            if (fa.getDlpStartDate() == null) {
                fa.setDlpStartDate(LocalDate.now());
            }
            FinalAccount saved = finalAccountRepository.save(fa);
            eventPublisher.publishEvent(
                    new FinalAccountAgreedEvent(this, saved.getId(), projectId, req.agreedBy()));
            logger.info("Final account AGREED for project {} by {}", projectId, req.agreedBy());
            return saved;
        }

        logger.info("Final account status → {} for project {}", req.targetStatus(), projectId);
        return finalAccountRepository.save(fa);
    }

    // -------------------------------------------------------------------------
    // Retention release
    // -------------------------------------------------------------------------

    public FinalAccount releaseRetention(Long projectId, ReleaseRetentionRequest req) {
        FinalAccount fa = finalAccountRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No final account for project: " + projectId));

        if (!fa.isAgreed() && !fa.isClosed()) {
            throw new IllegalStateException("Retention can only be released for an AGREED or CLOSED final account.");
        }
        if (fa.isRetentionReleased()) {
            throw new IllegalStateException("Retention has already been released.");
        }
        if (fa.getDlpEndDate() != null && fa.getDlpEndDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException(
                    "DLP period has not ended yet. DLP end date: " + fa.getDlpEndDate());
        }

        fa.setRetentionReleased(true);
        fa.setRetentionReleaseDate(req.releaseDate());
        logger.info("Retention released for project {} by {} on {}", projectId, req.releasedBy(), req.releaseDate());
        return finalAccountRepository.save(fa);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FinalAccount requireStatus(Long projectId, FinalAccountStatus required) {
        FinalAccount fa = finalAccountRepository.findByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No final account for project: " + projectId));
        if (fa.getStatus() != required) {
            throw new IllegalStateException(
                    "Operation requires status " + required + ". Current: " + fa.getStatus());
        }
        return fa;
    }

    private void validateTransition(FinalAccountStatus current, FinalAccountStatus target) {
        boolean valid = switch (current) {
            case DRAFT      -> target == FinalAccountStatus.SUBMITTED;
            case SUBMITTED  -> target == FinalAccountStatus.AGREED || target == FinalAccountStatus.DISPUTED;
            case DISPUTED   -> target == FinalAccountStatus.SUBMITTED || target == FinalAccountStatus.AGREED;
            case AGREED     -> target == FinalAccountStatus.CLOSED;
            case CLOSED     -> false; // terminal
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition: " + current + " → " + target);
        }
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
