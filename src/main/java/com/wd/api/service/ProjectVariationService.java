package com.wd.api.service;

import com.wd.api.dto.ProjectVariationSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectVariationService {

    @Autowired
    private ProjectVariationRepository variationRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Transactional(readOnly = true)
    public Page<ProjectVariation> searchProjectVariations(ProjectVariationSearchFilter filter) {
        Specification<ProjectVariation> spec = buildSpecification(filter);
        return variationRepository.findAll(spec, filter.toPageable());
    }

    private Specification<ProjectVariation> buildSpecification(ProjectVariationSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across title, description, variation type
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern),
                    cb.like(cb.lower(root.get("variationType")), searchPattern)
                ));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by variationType
            if (filter.getVariationType() != null && !filter.getVariationType().isEmpty()) {
                predicates.add(cb.equal(root.get("variationType"), filter.getVariationType()));
            }

            // Filter by approvalStatus
            if (filter.getApprovalStatus() != null && !filter.getApprovalStatus().isEmpty()) {
                try {
                    VariationStatus status = VariationStatus.valueOf(filter.getApprovalStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // Invalid status, skip
                }
            }

            // Filter by requestedById
            if (filter.getRequestedById() != null) {
                predicates.add(cb.equal(root.get("createdByUserId"), filter.getRequestedById()));
            }

            // Filter by approvedById
            if (filter.getApprovedById() != null) {
                predicates.add(cb.equal(root.get("approvedByUserId"), filter.getApprovedById()));
            }

            // Filter by status (from base class)
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                try {
                    VariationStatus status = VariationStatus.valueOf(filter.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // Invalid status, skip
                }
            }

            // Amount range filter
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("estimatedCost"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("estimatedCost"), filter.getMaxAmount()));
            }

            // Date range filter
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate().atStartOfDay()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<ProjectVariation> getVariationsByProject(Long projectId) {
        return variationRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectVariation createVariation(ProjectVariation variation, Long projectId, Long createdById) {
        CustomerProject project = projectRepository
                .findById(Objects.requireNonNull(projectId, "Project ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        variation.setProject(project);

        if (createdById != null) {
            variation.setCreatedByUserId(createdById);
        }

        variation.setStatus(VariationStatus.DRAFT);

        return variationRepository.save(variation);
    }

    @Transactional
    public ProjectVariation updateVariation(Long id, ProjectVariation details) {
        ProjectVariation existing = variationRepository.findById(Objects.requireNonNull(id, "Variation ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + id));

        if (existing.getStatus() != VariationStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit a variation that is already submitted or approved.");
        }

        existing.setDescription(details.getDescription());
        existing.setEstimatedAmount(details.getEstimatedAmount());
        existing.setNotes(details.getNotes());

        return variationRepository.save(existing);
    }

    @Transactional
    public ProjectVariation deleteVariation(Long id) {
        ProjectVariation existing = variationRepository.findById(Objects.requireNonNull(id, "Variation ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + id));

        if (existing.getStatus() != VariationStatus.DRAFT) {
            throw new IllegalStateException("Cannot delete a variation that is already submitted or approved.");
        }

        variationRepository.delete(existing);
        return existing;
    }

    @Transactional
    public ProjectVariation submitForApproval(Long id) {
        ProjectVariation existing = variationRepository.findById(Objects.requireNonNull(id, "Variation ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + id));

        existing.setStatus(VariationStatus.PENDING_APPROVAL);
        return variationRepository.save(existing);
    }

    @Transactional
    public ProjectVariation approveVariation(Long id, Long approverId) {
        ProjectVariation existing = variationRepository.findById(Objects.requireNonNull(id, "Variation ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + id));

        PortalUser approver = portalUserRepository
                .findById(Objects.requireNonNull(approverId, "Approver ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        existing.setStatus(VariationStatus.APPROVED);
        existing.setClientApproved(true);
        existing.setApprovedBy(approver);
        existing.setApprovedAt(LocalDateTime.now());

        // TODO: Logic to update project budget or create additional invoice could go
        // here

        return variationRepository.save(existing);
    }

    @Transactional
    public ProjectVariation rejectVariation(Long id, Long approverId, String reason) {
        ProjectVariation existing = variationRepository.findById(Objects.requireNonNull(id, "Variation ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Variation not found: " + id));

        existing.setStatus(VariationStatus.REJECTED);
        existing.setClientApproved(false);
        existing.setNotes(existing.getNotes() + "\nRejection Reason: " + reason);

        return variationRepository.save(existing);
    }
}
