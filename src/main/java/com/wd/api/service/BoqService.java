package com.wd.api.service;

import com.wd.api.dto.BoqSearchFilter;
import com.wd.api.model.BoqItem;
import com.wd.api.repository.BoqItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BoqService {

    @Autowired
    private BoqItemRepository boqItemRepository;

    @Transactional(readOnly = true)
    public Page<BoqItem> searchBoqItems(BoqSearchFilter filter) {
        Specification<BoqItem> spec = buildSpecification(filter);
        return boqItemRepository.findAll(spec, filter.toPageable());
    }

    private Specification<BoqItem> buildSpecification(BoqSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across description, itemCode
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), searchPattern),
                    cb.like(cb.lower(root.get("itemCode")), searchPattern)
                ));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by workTypeId
            if (filter.getWorkTypeId() != null) {
                predicates.add(cb.equal(root.get("workType").get("id"), filter.getWorkTypeId()));
            }

            // Filter by itemCode
            if (filter.getItemCode() != null && !filter.getItemCode().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("itemCode")), "%" + filter.getItemCode().toLowerCase() + "%"));
            }

            // Amount range filter
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.getMinAmount()));
            }
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), filter.getMaxAmount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public List<BoqItem> getProjectBoq(Long projectId) {
        return boqItemRepository.findByProjectId(projectId);
    }

    @Transactional
    public BoqItem updateBoqItem(Long id, BoqItem details) {
        BoqItem item = boqItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BoQ Item not found: " + id));

        item.setQuantity(details.getQuantity());
        item.setUnitRate(details.getUnitRate());
        item.setNotes(details.getNotes());
        // Trigger recalculation handled by Entity @PreUpdate/PrePersist if logic exists
        // there,
        // or manually here if needed. The entity has calculateTotalAmount() called on
        // hooks.

        return boqItemRepository.save(item);
    }

    // Note: Items are usually pre-populated or imported, but we can add create if
    // needed.
    // For now assuming viewing and updating actuals/notes.
}
