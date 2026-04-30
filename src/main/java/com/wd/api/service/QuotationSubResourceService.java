package com.wd.api.service;

import com.wd.api.dto.quotation.AssumptionRequest;
import com.wd.api.dto.quotation.ExclusionRequest;
import com.wd.api.dto.quotation.InclusionRequest;
import com.wd.api.dto.quotation.PaymentMilestoneRequest;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.QuotationAssumption;
import com.wd.api.model.QuotationExclusion;
import com.wd.api.model.QuotationInclusion;
import com.wd.api.model.QuotationPaymentMilestone;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.QuotationAssumptionRepository;
import com.wd.api.repository.QuotationExclusionRepository;
import com.wd.api.repository.QuotationInclusionRepository;
import com.wd.api.repository.QuotationPaymentMilestoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for the four sub-resources attached to a {@link LeadQuotation}
 * by V76 — inclusions, exclusions, assumptions, and payment milestones.
 *
 * <p>Kept deliberately separate from {@code LeadQuotationService} (already
 * 978 lines) so the redesign starts with a clean split. Each sub-resource
 * follows the same shape: list / create / update / delete, with owner-quote
 * validation on every write to prevent cross-quote tampering via guessed IDs.
 *
 * <p>For payment milestones the service adds a soft "sum-of-percentages"
 * sanity check on create/update — it won't reject mid-edit (the user might
 * be on milestone 3 of 8 and not yet at 100%), but it surfaces the running
 * total so the controller can warn the UI when needed.
 */
@Service
public class QuotationSubResourceService {

    private final LeadQuotationRepository quotationRepository;
    private final QuotationInclusionRepository inclusionRepository;
    private final QuotationExclusionRepository exclusionRepository;
    private final QuotationAssumptionRepository assumptionRepository;
    private final QuotationPaymentMilestoneRepository milestoneRepository;

    public QuotationSubResourceService(
            LeadQuotationRepository quotationRepository,
            QuotationInclusionRepository inclusionRepository,
            QuotationExclusionRepository exclusionRepository,
            QuotationAssumptionRepository assumptionRepository,
            QuotationPaymentMilestoneRepository milestoneRepository) {
        this.quotationRepository = quotationRepository;
        this.inclusionRepository = inclusionRepository;
        this.exclusionRepository = exclusionRepository;
        this.assumptionRepository = assumptionRepository;
        this.milestoneRepository = milestoneRepository;
    }

    // ── Inclusions ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QuotationInclusion> listInclusions(Long quotationId) {
        requireQuotation(quotationId);
        return inclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId);
    }

    @Transactional
    public QuotationInclusion createInclusion(Long quotationId, InclusionRequest req) {
        LeadQuotation quotation = requireQuotation(quotationId);
        QuotationInclusion entity = new QuotationInclusion();
        entity.setQuotation(quotation);
        entity.setDisplayOrder(resolveDisplayOrder(
                req.displayOrder(),
                inclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId).size()));
        entity.setCategory(req.category());
        entity.setText(req.text());
        return inclusionRepository.save(entity);
    }

    @Transactional
    public QuotationInclusion updateInclusion(Long quotationId, Long inclusionId, InclusionRequest req) {
        QuotationInclusion entity = inclusionRepository.findById(inclusionId)
                .orElseThrow(() -> new IllegalArgumentException("Inclusion not found: " + inclusionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Inclusion");
        if (req.displayOrder() != null) entity.setDisplayOrder(req.displayOrder());
        entity.setCategory(req.category());
        entity.setText(req.text());
        return inclusionRepository.save(entity);
    }

    @Transactional
    public void deleteInclusion(Long quotationId, Long inclusionId) {
        QuotationInclusion entity = inclusionRepository.findById(inclusionId)
                .orElseThrow(() -> new IllegalArgumentException("Inclusion not found: " + inclusionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Inclusion");
        inclusionRepository.delete(entity);
    }

    // ── Exclusions ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QuotationExclusion> listExclusions(Long quotationId) {
        requireQuotation(quotationId);
        return exclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId);
    }

    @Transactional
    public QuotationExclusion createExclusion(Long quotationId, ExclusionRequest req) {
        LeadQuotation quotation = requireQuotation(quotationId);
        QuotationExclusion entity = new QuotationExclusion();
        entity.setQuotation(quotation);
        entity.setDisplayOrder(resolveDisplayOrder(
                req.displayOrder(),
                exclusionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId).size()));
        entity.setText(req.text());
        entity.setCostImplicationNote(req.costImplicationNote());
        return exclusionRepository.save(entity);
    }

    @Transactional
    public QuotationExclusion updateExclusion(Long quotationId, Long exclusionId, ExclusionRequest req) {
        QuotationExclusion entity = exclusionRepository.findById(exclusionId)
                .orElseThrow(() -> new IllegalArgumentException("Exclusion not found: " + exclusionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Exclusion");
        if (req.displayOrder() != null) entity.setDisplayOrder(req.displayOrder());
        entity.setText(req.text());
        entity.setCostImplicationNote(req.costImplicationNote());
        return exclusionRepository.save(entity);
    }

    @Transactional
    public void deleteExclusion(Long quotationId, Long exclusionId) {
        QuotationExclusion entity = exclusionRepository.findById(exclusionId)
                .orElseThrow(() -> new IllegalArgumentException("Exclusion not found: " + exclusionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Exclusion");
        exclusionRepository.delete(entity);
    }

    // ── Assumptions ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QuotationAssumption> listAssumptions(Long quotationId) {
        requireQuotation(quotationId);
        return assumptionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId);
    }

    @Transactional
    public QuotationAssumption createAssumption(Long quotationId, AssumptionRequest req) {
        LeadQuotation quotation = requireQuotation(quotationId);
        QuotationAssumption entity = new QuotationAssumption();
        entity.setQuotation(quotation);
        entity.setDisplayOrder(resolveDisplayOrder(
                req.displayOrder(),
                assumptionRepository.findByQuotationIdOrderByDisplayOrderAsc(quotationId).size()));
        entity.setText(req.text());
        return assumptionRepository.save(entity);
    }

    @Transactional
    public QuotationAssumption updateAssumption(Long quotationId, Long assumptionId, AssumptionRequest req) {
        QuotationAssumption entity = assumptionRepository.findById(assumptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assumption not found: " + assumptionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Assumption");
        if (req.displayOrder() != null) entity.setDisplayOrder(req.displayOrder());
        entity.setText(req.text());
        return assumptionRepository.save(entity);
    }

    @Transactional
    public void deleteAssumption(Long quotationId, Long assumptionId) {
        QuotationAssumption entity = assumptionRepository.findById(assumptionId)
                .orElseThrow(() -> new IllegalArgumentException("Assumption not found: " + assumptionId));
        ensureBelongs(entity.getQuotation(), quotationId, "Assumption");
        assumptionRepository.delete(entity);
    }

    // ── Payment milestones ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QuotationPaymentMilestone> listMilestones(Long quotationId) {
        requireQuotation(quotationId);
        return milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(quotationId);
    }

    @Transactional
    public QuotationPaymentMilestone createMilestone(Long quotationId, PaymentMilestoneRequest req) {
        LeadQuotation quotation = requireQuotation(quotationId);
        // Service-layer dedupe of milestone_number — the unique constraint
        // would otherwise surface as an ugly DataIntegrityViolation.
        boolean numberTaken = milestoneRepository
                .findByQuotationIdOrderByMilestoneNumberAsc(quotationId).stream()
                .anyMatch(m -> req.milestoneNumber().equals(m.getMilestoneNumber()));
        if (numberTaken) {
            throw new IllegalStateException(
                    "Milestone number " + req.milestoneNumber() + " already exists on this quotation");
        }
        QuotationPaymentMilestone entity = new QuotationPaymentMilestone();
        entity.setQuotation(quotation);
        entity.setMilestoneNumber(req.milestoneNumber());
        entity.setTriggerEvent(req.triggerEvent());
        entity.setPercentage(req.percentage());
        entity.setAmount(resolveMilestoneAmount(req.amount(), quotation, req.percentage()));
        entity.setNotes(req.notes());
        return milestoneRepository.save(entity);
    }

    @Transactional
    public QuotationPaymentMilestone updateMilestone(
            Long quotationId, Long milestoneId, PaymentMilestoneRequest req) {
        QuotationPaymentMilestone entity = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));
        ensureBelongs(entity.getQuotation(), quotationId, "Milestone");
        // Allow renumbering, but prevent collisions with other rows on the same parent.
        if (!req.milestoneNumber().equals(entity.getMilestoneNumber())) {
            boolean clash = milestoneRepository
                    .findByQuotationIdOrderByMilestoneNumberAsc(quotationId).stream()
                    .anyMatch(m -> !m.getId().equals(milestoneId)
                            && req.milestoneNumber().equals(m.getMilestoneNumber()));
            if (clash) {
                throw new IllegalStateException(
                        "Milestone number " + req.milestoneNumber() + " already exists on this quotation");
            }
        }
        entity.setMilestoneNumber(req.milestoneNumber());
        entity.setTriggerEvent(req.triggerEvent());
        entity.setPercentage(req.percentage());
        entity.setAmount(resolveMilestoneAmount(req.amount(), entity.getQuotation(), req.percentage()));
        entity.setNotes(req.notes());
        return milestoneRepository.save(entity);
    }

    @Transactional
    public void deleteMilestone(Long quotationId, Long milestoneId) {
        QuotationPaymentMilestone entity = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));
        ensureBelongs(entity.getQuotation(), quotationId, "Milestone");
        milestoneRepository.delete(entity);
    }

    /**
     * Running sum of percentage across milestones on a quotation. Surfaced
     * by the controller so the Flutter editor can show a "you are at 85% —
     * 15% remaining" hint. Matters because the contract is invalid if the
     * milestones don't sum to 100.
     */
    @Transactional(readOnly = true)
    public BigDecimal totalMilestonePercentage(Long quotationId) {
        return milestoneRepository.findByQuotationIdOrderByMilestoneNumberAsc(quotationId)
                .stream()
                .map(QuotationPaymentMilestone::getPercentage)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private LeadQuotation requireQuotation(Long quotationId) {
        return quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));
    }

    private void ensureBelongs(LeadQuotation parent, Long expectedQuotationId, String label) {
        if (parent == null || !expectedQuotationId.equals(parent.getId())) {
            // Treat as not-found rather than 403 to avoid leaking which IDs exist.
            throw new IllegalArgumentException(label + " does not belong to quotation " + expectedQuotationId);
        }
    }

    private Integer resolveDisplayOrder(Integer requested, int existingCount) {
        return requested != null ? requested : existingCount;
    }

    /**
     * If the caller supplied an explicit rupee figure, use it. Otherwise,
     * derive from the parent quotation's {@code finalAmount} when known —
     * BUDGETARY parents typically have no final amount, in which case the
     * milestone keeps {@code amount = null} and only the percentage is
     * meaningful.
     */
    private BigDecimal resolveMilestoneAmount(
            BigDecimal explicitAmount, LeadQuotation quotation, BigDecimal percentage) {
        if (explicitAmount != null) return explicitAmount;
        BigDecimal parentTotal = quotation.getFinalAmount();
        if (parentTotal == null || percentage == null) return null;
        return parentTotal
                .multiply(percentage)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }
}
