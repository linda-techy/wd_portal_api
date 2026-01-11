package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
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

    @Transactional(readOnly = true)
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
