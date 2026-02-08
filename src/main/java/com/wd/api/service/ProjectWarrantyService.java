package com.wd.api.service;

import com.wd.api.dto.ProjectWarrantySearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectWarranty;
import com.wd.api.model.enums.WarrantyStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectWarrantyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectWarrantyService {

    @Autowired
    private ProjectWarrantyRepository warrantyRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<ProjectWarranty> searchProjectWarranties(ProjectWarrantySearchFilter filter) {
        Specification<ProjectWarranty> spec = buildSpecification(filter);
        return warrantyRepository.findAll(spec, filter.toPageable());
    }

    private Specification<ProjectWarranty> buildSpecification(ProjectWarrantySearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across warranty type, description
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("warrantyType")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("terms")), searchPattern)));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by warrantyType
            if (filter.getWarrantyType() != null && !filter.getWarrantyType().isEmpty()) {
                predicates.add(cb.equal(root.get("warrantyType"), filter.getWarrantyType()));
            }

            // Filter by activeOnly (endDate >= today)
            if (filter.isActiveOnly()) {
                LocalDate today = LocalDate.now();
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));
                predicates.add(cb.equal(root.get("status"), WarrantyStatus.ACTIVE));
            }

            // Filter by expiredOnly (endDate < today)
            if (filter.getExpiredOnly() != null && filter.getExpiredOnly()) {
                LocalDate today = LocalDate.now();
                predicates.add(cb.lessThan(root.get("endDate"), today));
            }

            // Filter by status (from base class)
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                try {
                    WarrantyStatus status = WarrantyStatus.valueOf(filter.getStatus().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // Invalid status, skip
                }
            }

            // Date range filter on startDate
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.getEndDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<ProjectWarranty> getWarrantiesByProject(Long projectId) {
        if (projectId == null)
            return java.util.Collections.emptyList();
        return warrantyRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectWarranty createWarranty(ProjectWarranty warranty, Long projectId) {
        if (projectId == null)
            throw new IllegalArgumentException("Project ID cannot be null");
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        warranty.setProject(project);

        // Auto-calculate status if null
        if (warranty.getStatus() == null) {
            warranty.setStatus(WarrantyStatus.ACTIVE);
        }

        return warrantyRepository.save(warranty);
    }

    @Transactional
    public ProjectWarranty updateWarranty(Long id, ProjectWarranty details) {
        if (id == null)
            throw new IllegalArgumentException("Warranty ID cannot be null");
        ProjectWarranty existing = warrantyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warranty not found: " + id));

        existing.setComponentName(details.getComponentName());
        existing.setProviderName(details.getProviderName());
        existing.setDescription(details.getDescription());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setStatus(details.getStatus());
        existing.setCoverageDetails(details.getCoverageDetails());

        return warrantyRepository.save(existing);
    }

    @Transactional
    public void deleteWarranty(Long id) {
        if (id != null) {
            warrantyRepository.deleteById(id);
        }
    }
}
