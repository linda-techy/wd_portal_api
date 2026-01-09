package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
