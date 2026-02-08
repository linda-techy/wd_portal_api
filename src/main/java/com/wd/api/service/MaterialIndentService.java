package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import com.wd.api.dto.MaterialIndentSearchFilter;
import com.wd.api.util.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialIndentService {

    private final MaterialIndentRepository indentRepository;
    private final CustomerProjectRepository projectRepository;
    private final MaterialRepository materialRepository;
    // Inject User Service usually to get Current User, but for now we'll rely on
    // passed IDs or SecurityContext

    @Transactional
    @SuppressWarnings("null")
    public MaterialIndent createIndent(Long projectId, MaterialIndent indent, Long requestedById) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (indent.getIndentNumber() == null) {
            indent.setIndentNumber(generateIndentNumber(project));
        }

        indent.setProject(project);
        indent.setRequestedById(requestedById);
        indent.setStatus(MaterialIndent.IndentStatus.DRAFT);

        // Link items
        if (indent.getItems() != null) {
            indent.getItems().forEach(item -> {
                item.setIndent(indent);
                if (item.getMaterial() != null && item.getMaterial().getId() != null) {
                    Material mat = materialRepository.findById(item.getMaterial().getId())
                            .orElseThrow(
                                    () -> new RuntimeException("Material not found: " + item.getMaterial().getId()));
                    item.setMaterial(mat);
                    // Default item name/unit from Material if missing
                    if (item.getItemName() == null)
                        item.setItemName(mat.getName());
                    if (item.getUnit() == null)
                        item.setUnit(mat.getUnit().name());
                }
            });
        }

        return indentRepository.save(indent);
    }

    @Transactional
    @SuppressWarnings("null")
    public MaterialIndent submitIndent(Long indentId) {
        MaterialIndent indent = indentRepository.findById(indentId)
                .orElseThrow(() -> new RuntimeException("Indent not found"));

        if (indent.getStatus() != MaterialIndent.IndentStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT indents can be submitted");
        }

        indent.setStatus(MaterialIndent.IndentStatus.SUBMITTED);
        return indentRepository.save(indent);
    }

    @Transactional
    @SuppressWarnings("null")
    public MaterialIndent approveIndent(Long indentId, Long approvedById) {
        MaterialIndent indent = indentRepository.findById(indentId)
                .orElseThrow(() -> new RuntimeException("Indent not found"));

        if (indent.getStatus() != MaterialIndent.IndentStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED indents can be approved");
        }

        indent.setStatus(MaterialIndent.IndentStatus.APPROVED);
        indent.setApprovedById(approvedById);
        indent.setApprovedAt(LocalDateTime.now());

        // Auto-set approved quantity to requested quantity if not set
        indent.getItems().forEach(item -> {
            if (item.getQuantityApproved() == null) {
                item.setQuantityApproved(item.getQuantityRequested());
            }
        });

        return indentRepository.save(indent);
    }

    /**
     * NEW: Standardized search method using MaterialIndentSearchFilter
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<MaterialIndent> search(MaterialIndentSearchFilter filter) {
        Specification<MaterialIndent> spec = buildSearchSpecification(filter);
        return indentRepository.findAll(spec, filter.toPageable());
    }

    /**
     * Build JPA Specification from MaterialIndentSearchFilter
     */
    private Specification<MaterialIndent> buildSearchSpecification(MaterialIndentSearchFilter filter) {
        SpecificationBuilder<MaterialIndent> builder = new SpecificationBuilder<>();

        // Search across multiple fields
        Specification<MaterialIndent> searchSpec = builder.buildSearch(
                filter.getSearchQuery(),
                "indentNumber", "description", "notes");

        // Apply filters
        Specification<MaterialIndent> statusSpec = null;
        if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
            statusSpec = (root, query, cb) -> cb.equal(root.get("status"),
                    MaterialIndent.IndentStatus.valueOf(filter.getStatus().toUpperCase()));
        }

        // Project filter
        Specification<MaterialIndent> projectSpec = null;
        if (filter.getProjectId() != null) {
            projectSpec = (root, query, cb) -> cb.equal(root.get("project").get("id"), filter.getProjectId());
        }

        // Requester filter
        Specification<MaterialIndent> requesterSpec = builder.buildEquals("requestedById", filter.getRequestedBy());

        // Approver filter
        Specification<MaterialIndent> approverSpec = builder.buildEquals("approvedById", filter.getApprovedBy());

        // Indent number filter (partial match)
        Specification<MaterialIndent> indentNumberSpec = builder.buildLike("indentNumber", filter.getIndentNumber());

        // Date range (on createdAt)
        Specification<MaterialIndent> dateRangeSpec = null;
        if (filter.getStartDate() != null || filter.getEndDate() != null) {
            dateRangeSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getStartDate().atStartOfDay()));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            root.get("createdAt"),
                            filter.getEndDate().plusDays(1).atStartOfDay()));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }

        // Exclude soft deleted
        Specification<MaterialIndent> notDeletedSpec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        // Combine all specifications
        return builder.and(
                searchSpec,
                statusSpec,
                projectSpec,
                requesterSpec,
                approverSpec,
                indentNumberSpec,
                dateRangeSpec,
                notDeletedSpec);
    }

    /**
     * DEPRECATED: Use search() instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<MaterialIndent> searchIndents(Long projectId, String status, String searchTerm, Pageable pageable) {
        Specification<MaterialIndent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft deleted if BaseEntity supports it (checking BaseEntity...)
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }

            if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), MaterialIndent.IndentStatus.valueOf(status)));
            }

            if (searchTerm != null && !searchTerm.isEmpty()) {
                String likePattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("indentNumber")), likePattern),
                        cb.like(cb.lower(root.get("project").get("name")), likePattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return indentRepository.findAll(spec, pageable);
    }

    public MaterialIndent getIndentById(Long id) {
        return indentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Indent not found"));
    }

    private String generateIndentNumber(CustomerProject project) {
        // Simple logic: IND-{ProjectCode}-{TimestampLast4}
        String pCode = project.getCode() != null ? project.getCode() : "PRJ";
        return "IND-" + pCode + "-" + (System.currentTimeMillis() % 100000);
    }
}
