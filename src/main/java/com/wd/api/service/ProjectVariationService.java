package com.wd.api.service;

import com.wd.api.dto.ProjectVariationSearchFilter;
import com.wd.api.model.ChangeRequestApprovalHistory;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.ChangeRequestApprovalHistoryRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Single source of truth for {@link ProjectVariation} reads + 8-state CR v2
 * transitions. Every transition: validate source -> mutate -> save ->
 * append history.
 */
@Service
public class ProjectVariationService {

    @Autowired private ProjectVariationRepository variationRepository;
    @Autowired private CustomerProjectRepository projectRepository;
    @Autowired private PortalUserRepository portalUserRepository;
    @Autowired private ChangeRequestApprovalHistoryRepository historyRepository;

    // ---- search / list / detail (unchanged behaviour) ----

    @Transactional(readOnly = true)
    public Page<ProjectVariation> searchProjectVariations(ProjectVariationSearchFilter filter) {
        return variationRepository.findAll(buildSpecification(filter), filter.toPageable());
    }

    private Specification<ProjectVariation> buildSpecification(ProjectVariationSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String pat = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pat),
                        cb.like(cb.lower(root.get("description")), pat),
                        cb.like(cb.lower(root.get("variationType")), pat)));
            }
            if (filter.getProjectId() != null)
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            if (filter.getVariationType() != null && !filter.getVariationType().isEmpty())
                predicates.add(cb.equal(root.get("variationType"), filter.getVariationType()));
            if (filter.getApprovalStatus() != null && !filter.getApprovalStatus().isEmpty()) {
                try { predicates.add(cb.equal(root.get("status"),
                        VariationStatus.valueOf(filter.getApprovalStatus().toUpperCase()))); }
                catch (IllegalArgumentException ignore) {}
            }
            if (filter.getRequestedById() != null)
                predicates.add(cb.equal(root.get("createdByUserId"), filter.getRequestedById()));
            if (filter.getApprovedById() != null)
                predicates.add(cb.equal(root.get("approvedByUserId"), filter.getApprovedById()));
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                try { predicates.add(cb.equal(root.get("status"),
                        VariationStatus.valueOf(filter.getStatus().toUpperCase()))); }
                catch (IllegalArgumentException ignore) {}
            }
            if (filter.getMinAmount() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("estimatedCost"), filter.getMinAmount()));
            if (filter.getMaxAmount() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("estimatedCost"), filter.getMaxAmount()));
            if (filter.getStartDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay()));
            if (filter.getEndDate() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atTime(23, 59, 59)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<ProjectVariation> getVariationsByProject(Long projectId) {
        return variationRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestApprovalHistory> getHistory(Long crId, Pageable pageable) {
        return historyRepository.findByChangeRequestIdOrderByActionAtDesc(crId, pageable);
    }

    // ---- create (DRAFT) ----

    @Transactional
    public ProjectVariation createVariation(ProjectVariation variation, Long projectId, Long createdById) {
        CustomerProject project = projectRepository
                .findById(Objects.requireNonNull(projectId, "Project ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        variation.setProject(project);
        if (createdById != null) variation.setCreatedByUserId(createdById);
        variation.setStatus(VariationStatus.DRAFT);
        return variationRepository.save(variation);
    }

    // ---- 8 transitions ----

    @Transactional
    public ProjectVariation submit(Long crId, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.DRAFT), "submit");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.SUBMITTED);
        cr.setSubmittedAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.SUBMITTED, actorUserId, null, null, null, null);
        return saved;
    }

    @Transactional
    public ProjectVariation cost(Long crId, BigDecimal costImpact, int timeImpactWorkingDays, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.SUBMITTED), "cost");
        VariationStatus from = cr.getStatus();
        cr.setCostImpact(costImpact);
        cr.setTimeImpactWorkingDays(timeImpactWorkingDays);
        cr.setStatus(VariationStatus.COSTED);
        cr.setCostedAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.COSTED, actorUserId, null, null, null, null);
        return saved;
    }

    @Transactional
    public ProjectVariation sendToCustomer(Long crId, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.COSTED), "sendToCustomer");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.CUSTOMER_APPROVAL_PENDING);
        cr.setSentToCustomerAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.CUSTOMER_APPROVAL_PENDING, actorUserId, null, null, null, null);
        // PR3 wires OTP generation + email here.
        return saved;
    }

    /**
     * Customer-side transition. Called by PR3's customer-API HMAC round-trip
     * after OTP verification succeeds. PR1 exposes the method but does not
     * wire the OTP flow.
     */
    @Transactional
    public ProjectVariation approveByCustomer(Long crId, Long customerUserId, String otpHash, String customerIp) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.CUSTOMER_APPROVAL_PENDING), "approveByCustomer");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.APPROVED);
        cr.setClientApproved(true);
        cr.setApprovedAt(LocalDateTime.now());
        // approvedBy stays null for customer-driven approval; the customer is
        // identified by customer_user_id on the history row.
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.APPROVED, null, customerUserId, otpHash, customerIp, null);
        return saved;
    }

    @Transactional
    public ProjectVariation schedule(Long crId, Long anchorTaskId, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.APPROVED), "schedule");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.SCHEDULED);
        cr.setScheduledAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.SCHEDULED, actorUserId, null, null, null,
                "anchorTaskId=" + anchorTaskId);
        // PR2 wires WBS merge here.
        return saved;
    }

    @Transactional
    public ProjectVariation start(Long crId, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.SCHEDULED), "start");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.IN_PROGRESS);
        cr.setStartedAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.IN_PROGRESS, actorUserId, null, null, null, null);
        return saved;
    }

    @Transactional
    public ProjectVariation complete(Long crId, Long actorUserId) {
        ProjectVariation cr = load(crId);
        assertSource(cr, EnumSet.of(VariationStatus.IN_PROGRESS), "complete");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.COMPLETE);
        cr.setCompletedAt(LocalDateTime.now());
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.COMPLETE, actorUserId, null, null, null, null);
        return saved;
    }

    private static final Set<VariationStatus> REJECTABLE = EnumSet.of(
            VariationStatus.DRAFT,
            VariationStatus.SUBMITTED,
            VariationStatus.COSTED,
            VariationStatus.CUSTOMER_APPROVAL_PENDING,
            VariationStatus.APPROVED,
            VariationStatus.SCHEDULED,
            VariationStatus.IN_PROGRESS);

    @Transactional
    public ProjectVariation reject(Long crId, String reason, Long actorUserId) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reject reason is required");
        }
        ProjectVariation cr = load(crId);
        assertSource(cr, REJECTABLE, "reject");
        VariationStatus from = cr.getStatus();
        cr.setStatus(VariationStatus.REJECTED);
        cr.setRejectionReason(reason);
        cr.setRejectedAt(LocalDateTime.now());
        cr.setClientApproved(false);
        ProjectVariation saved = variationRepository.save(cr);
        appendHistory(saved, from, VariationStatus.REJECTED, actorUserId, null, null, null, reason);
        return saved;
    }

    // ---- helpers ----

    private ProjectVariation load(Long crId) {
        return variationRepository.findById(Objects.requireNonNull(crId, "CR ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + crId));
    }

    private void assertSource(ProjectVariation cr, Set<VariationStatus> allowed, String op) {
        if (!allowed.contains(cr.getStatus())) {
            throw new IllegalStateException(
                    "Illegal " + op + " from status " + cr.getStatus() + "; allowed: " + allowed);
        }
    }

    private void appendHistory(ProjectVariation cr, VariationStatus from, VariationStatus to,
                               Long actorUserId, Long customerUserId,
                               String otpHash, String customerIp, String reason) {
        ChangeRequestApprovalHistory h = new ChangeRequestApprovalHistory();
        h.setChangeRequest(cr);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setActorUserId(actorUserId);
        h.setCustomerUserId(customerUserId);
        h.setOtpHash(otpHash);
        h.setCustomerIp(customerIp);
        h.setReason(reason);
        historyRepository.save(h);
    }

    // ---- legacy methods kept for in-flight callers; will be removed in S5 ----
    // updateVariation / deleteVariation: only allowed in DRAFT.

    @Transactional
    public ProjectVariation updateVariation(Long id, ProjectVariation details) {
        ProjectVariation existing = load(id);
        if (existing.getStatus() != VariationStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit a CR that is not in DRAFT.");
        }
        existing.setDescription(details.getDescription());
        existing.setEstimatedAmount(details.getEstimatedAmount());
        existing.setNotes(details.getNotes());
        return variationRepository.save(existing);
    }

    @Transactional
    public ProjectVariation deleteVariation(Long id) {
        ProjectVariation existing = load(id);
        if (existing.getStatus() != VariationStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete a CR that is not in DRAFT.");
        }
        variationRepository.delete(existing);
        return existing;
    }

    // ---- back-compat shims (used by pre-S4 controller endpoints; deprecated) ----

    /** @deprecated Use {@link #submit(Long, Long)}. Removed in S5. */
    @Deprecated
    @Transactional
    public ProjectVariation submitForApproval(Long id) {
        return submit(id, null);
    }

    /** @deprecated Use {@link #approveByCustomer(Long, Long, String, String)}. Removed in S5. */
    @Deprecated
    @Transactional
    public ProjectVariation approveVariation(Long id, Long approverId) {
        ProjectVariation cr = approveByCustomer(id, null, null, null);
        if (approverId != null) {
            PortalUser approver = portalUserRepository.findById(approverId)
                    .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
            cr.setApprovedBy(approver);
            cr = variationRepository.save(cr);
        }
        return cr;
    }

    /** @deprecated Use {@link #reject(Long, String, Long)}. Removed in S5. */
    @Deprecated
    @Transactional
    public ProjectVariation rejectVariation(Long id, Long approverId, String reason) {
        return reject(id, reason, approverId);
    }
}
