package com.wd.api.estimation.service;

import com.wd.api.estimation.domain.*;
import com.wd.api.estimation.dto.*;
import com.wd.api.estimation.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class EstimationSubResourceService {

    private static final Logger log = LoggerFactory.getLogger(EstimationSubResourceService.class);

    private static final BigDecimal MILESTONE_SUM_MIN = new BigDecimal("99.0");
    private static final BigDecimal MILESTONE_SUM_MAX = new BigDecimal("101.0");

    private final EstimationInclusionRepository inclusionRepo;
    private final EstimationExclusionRepository exclusionRepo;
    private final EstimationAssumptionRepository assumptionRepo;
    private final EstimationPaymentMilestoneRepository milestoneRepo;

    public EstimationSubResourceService(
            EstimationInclusionRepository inclusionRepo,
            EstimationExclusionRepository exclusionRepo,
            EstimationAssumptionRepository assumptionRepo,
            EstimationPaymentMilestoneRepository milestoneRepo) {
        this.inclusionRepo  = inclusionRepo;
        this.exclusionRepo  = exclusionRepo;
        this.assumptionRepo = assumptionRepo;
        this.milestoneRepo  = milestoneRepo;
    }

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EstimationSubResourceResponse> list(UUID estimationId, SubResourceType type) {
        return switch (type) {
            case INCLUSION        -> inclusionRepo.findByEstimationIdOrderByDisplayOrderAsc(estimationId)
                    .stream().map(this::toResponse).toList();
            case EXCLUSION        -> exclusionRepo.findByEstimationIdOrderByDisplayOrderAsc(estimationId)
                    .stream().map(this::toResponse).toList();
            case ASSUMPTION       -> assumptionRepo.findByEstimationIdOrderByDisplayOrderAsc(estimationId)
                    .stream().map(this::toResponse).toList();
            case PAYMENT_MILESTONE -> milestoneRepo.findByEstimationIdOrderByDisplayOrderAsc(estimationId)
                    .stream().map(this::toResponse).toList();
        };
    }

    // -------------------------------------------------------------------------
    // Get single
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EstimationSubResourceResponse get(UUID estimationId, SubResourceType type, UUID id) {
        return switch (type) {
            case INCLUSION        -> toResponse(findInclusion(estimationId, id));
            case EXCLUSION        -> toResponse(findExclusion(estimationId, id));
            case ASSUMPTION       -> toResponse(findAssumption(estimationId, id));
            case PAYMENT_MILESTONE -> toResponse(findMilestone(estimationId, id));
        };
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public EstimationSubResourceResponse create(UUID estimationId, SubResourceType type,
                                                EstimationSubResourceRequest req) {
        validatePercentage(type, req);
        return switch (type) {
            case INCLUSION        -> {
                EstimationInclusion e = new EstimationInclusion();
                e.setEstimationId(estimationId);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(inclusionRepo.save(e));
            }
            case EXCLUSION        -> {
                EstimationExclusion e = new EstimationExclusion();
                e.setEstimationId(estimationId);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(exclusionRepo.save(e));
            }
            case ASSUMPTION       -> {
                EstimationAssumption e = new EstimationAssumption();
                e.setEstimationId(estimationId);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(assumptionRepo.save(e));
            }
            case PAYMENT_MILESTONE -> {
                EstimationPaymentMilestone e = new EstimationPaymentMilestone();
                e.setEstimationId(estimationId);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                e.setPercentage(req.percentage());
                EstimationPaymentMilestone saved = milestoneRepo.save(e);
                warnIfMilestoneSumOutOfRange(estimationId);
                yield toResponse(saved);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Transactional
    public EstimationSubResourceResponse update(UUID estimationId, SubResourceType type, UUID id,
                                                EstimationSubResourceRequest req) {
        validatePercentage(type, req);
        return switch (type) {
            case INCLUSION        -> {
                EstimationInclusion e = findInclusion(estimationId, id);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(inclusionRepo.save(e));
            }
            case EXCLUSION        -> {
                EstimationExclusion e = findExclusion(estimationId, id);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(exclusionRepo.save(e));
            }
            case ASSUMPTION       -> {
                EstimationAssumption e = findAssumption(estimationId, id);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                yield toResponse(assumptionRepo.save(e));
            }
            case PAYMENT_MILESTONE -> {
                EstimationPaymentMilestone e = findMilestone(estimationId, id);
                e.setLabel(req.label());
                e.setDescription(req.description());
                e.setDisplayOrder(req.displayOrder());
                e.setPercentage(req.percentage());
                EstimationPaymentMilestone saved = milestoneRepo.save(e);
                warnIfMilestoneSumOutOfRange(estimationId);
                yield toResponse(saved);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Transactional
    public void delete(UUID estimationId, SubResourceType type, UUID id) {
        switch (type) {
            case INCLUSION        -> inclusionRepo.delete(findInclusion(estimationId, id));
            case EXCLUSION        -> exclusionRepo.delete(findExclusion(estimationId, id));
            case ASSUMPTION       -> assumptionRepo.delete(findAssumption(estimationId, id));
            case PAYMENT_MILESTONE -> {
                milestoneRepo.delete(findMilestone(estimationId, id));
                warnIfMilestoneSumOutOfRange(estimationId);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — finders
    // -------------------------------------------------------------------------

    private EstimationInclusion findInclusion(UUID estimationId, UUID id) {
        EstimationInclusion e = inclusionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inclusion not found: " + id));
        if (!e.getEstimationId().equals(estimationId)) {
            throw new IllegalArgumentException("Inclusion " + id + " does not belong to estimation " + estimationId);
        }
        return e;
    }

    private EstimationExclusion findExclusion(UUID estimationId, UUID id) {
        EstimationExclusion e = exclusionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exclusion not found: " + id));
        if (!e.getEstimationId().equals(estimationId)) {
            throw new IllegalArgumentException("Exclusion " + id + " does not belong to estimation " + estimationId);
        }
        return e;
    }

    private EstimationAssumption findAssumption(UUID estimationId, UUID id) {
        EstimationAssumption e = assumptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assumption not found: " + id));
        if (!e.getEstimationId().equals(estimationId)) {
            throw new IllegalArgumentException("Assumption " + id + " does not belong to estimation " + estimationId);
        }
        return e;
    }

    private EstimationPaymentMilestone findMilestone(UUID estimationId, UUID id) {
        EstimationPaymentMilestone e = milestoneRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PaymentMilestone not found: " + id));
        if (!e.getEstimationId().equals(estimationId)) {
            throw new IllegalArgumentException("PaymentMilestone " + id + " does not belong to estimation " + estimationId);
        }
        return e;
    }

    // -------------------------------------------------------------------------
    // Helpers — validation / warnings
    // -------------------------------------------------------------------------

    private void validatePercentage(SubResourceType type, EstimationSubResourceRequest req) {
        if (type == SubResourceType.PAYMENT_MILESTONE) {
            if (req.percentage() == null) {
                throw new IllegalArgumentException("percentage is required for PAYMENT_MILESTONE");
            }
        } else {
            if (req.percentage() != null) {
                throw new IllegalArgumentException("percentage must not be provided for type " + type);
            }
        }
    }

    private void warnIfMilestoneSumOutOfRange(UUID estimationId) {
        BigDecimal sum = milestoneRepo.findByEstimationIdOrderByDisplayOrderAsc(estimationId)
                .stream()
                .map(EstimationPaymentMilestone::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(MILESTONE_SUM_MIN) < 0 || sum.compareTo(MILESTONE_SUM_MAX) > 0) {
            log.warn("Payment milestone percentages for estimation {} sum to {} — expected ~100%%",
                    estimationId, sum);
        }
    }

    // -------------------------------------------------------------------------
    // Converters
    // -------------------------------------------------------------------------

    private EstimationSubResourceResponse toResponse(EstimationInclusion e) {
        return new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                e.getLabel(), e.getDescription(), e.getDisplayOrder(), null);
    }

    private EstimationSubResourceResponse toResponse(EstimationExclusion e) {
        return new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                e.getLabel(), e.getDescription(), e.getDisplayOrder(), null);
    }

    private EstimationSubResourceResponse toResponse(EstimationAssumption e) {
        return new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                e.getLabel(), e.getDescription(), e.getDisplayOrder(), null);
    }

    private EstimationSubResourceResponse toResponse(EstimationPaymentMilestone e) {
        return new EstimationSubResourceResponse(e.getId(), e.getEstimationId(),
                e.getLabel(), e.getDescription(), e.getDisplayOrder(), e.getPercentage());
    }
}
