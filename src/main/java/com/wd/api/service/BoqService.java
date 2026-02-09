package com.wd.api.service;

import com.wd.api.dto.BoqSearchFilter;
import com.wd.api.model.BoqItem;
import com.wd.api.model.BoqWorkType;
import com.wd.api.model.CustomerProject;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.BoqWorkTypeRepository;
import com.wd.api.repository.CustomerProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoqService {

    @Autowired
    private BoqItemRepository boqItemRepository;

    @Autowired
    private BoqWorkTypeRepository boqWorkTypeRepository;

    @Autowired
    private CustomerProjectRepository customerProjectRepository;

    @Transactional(readOnly = true)
    public Page<BoqItem> searchBoqItems(BoqSearchFilter filter) {
        Specification<BoqItem> spec = buildSpecification(filter);
        return boqItemRepository.findAll(spec, Objects.requireNonNull(filter.toPageable()));
    }

    private Specification<BoqItem> buildSpecification(BoqSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.get("notes")), searchPattern)));
            }

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            if (filter.getWorkTypeId() != null) {
                predicates.add(cb.equal(root.get("workType").get("id"), filter.getWorkTypeId()));
            }

            if (filter.getItemCode() != null && !filter.getItemCode().isEmpty()) {
                predicates.add(
                        cb.like(cb.lower(root.get("description")),
                                "%" + filter.getItemCode().toLowerCase() + "%"));
            }

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
    public BoqItem createBoqItem(Map<String, Object> request) {
        Long projectId = ((Number) request.get("projectId")).longValue();
        CustomerProject project = customerProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        BoqItem item = new BoqItem();
        item.setProject(project);
        item.setDescription((String) request.get("description"));
        item.setUnit((String) request.get("unit"));

        if (request.get("quantity") != null) {
            item.setQuantity(new BigDecimal(request.get("quantity").toString()));
        }
        if (request.get("unitRate") != null) {
            item.setUnitRate(new BigDecimal(request.get("unitRate").toString()));
        }
        if (request.get("notes") != null) {
            item.setNotes((String) request.get("notes"));
        }

        // Link work type if provided
        if (request.get("workTypeId") != null) {
            Long workTypeId = ((Number) request.get("workTypeId")).longValue();
            BoqWorkType workType = boqWorkTypeRepository.findById(workTypeId)
                    .orElseThrow(() -> new RuntimeException("Work type not found: " + workTypeId));
            item.setWorkType(workType);
        }

        return boqItemRepository.save(item);
    }

    @Transactional
    public BoqItem updateBoqItem(Long id, Map<String, Object> details) {
        BoqItem item = boqItemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("BoQ Item not found: " + id));

        if (details.containsKey("description")) {
            item.setDescription((String) details.get("description"));
        }
        if (details.containsKey("unit")) {
            item.setUnit((String) details.get("unit"));
        }
        if (details.containsKey("quantity") && details.get("quantity") != null) {
            item.setQuantity(new BigDecimal(details.get("quantity").toString()));
        }
        if (details.containsKey("unitRate") && details.get("unitRate") != null) {
            item.setUnitRate(new BigDecimal(details.get("unitRate").toString()));
        }
        if (details.containsKey("notes")) {
            item.setNotes((String) details.get("notes"));
        }
        if (details.containsKey("workTypeId") && details.get("workTypeId") != null) {
            Long workTypeId = ((Number) details.get("workTypeId")).longValue();
            BoqWorkType workType = boqWorkTypeRepository.findById(workTypeId)
                    .orElseThrow(() -> new RuntimeException("Work type not found: " + workTypeId));
            item.setWorkType(workType);
        }

        return Objects.requireNonNull(boqItemRepository.save(item));
    }

    @Transactional
    public void deleteBoqItem(Long id) {
        BoqItem item = boqItemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("BoQ Item not found: " + id));
        boqItemRepository.delete(Objects.requireNonNull(item));
    }

    @Transactional(readOnly = true)
    public List<BoqWorkType> getAllWorkTypes() {
        return boqWorkTypeRepository.findAll(Sort.by("displayOrder", "name"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProjectSummary(Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectId(projectId);

        BigDecimal totalAmount = items.stream()
                .filter(i -> i.getTotalAmount() != null)
                .map(BoqItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by work type
        Map<String, BigDecimal> workTypeTotals = items.stream()
                .filter(i -> i.getWorkType() != null && i.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getWorkType().getName(),
                        Collectors.reducing(BigDecimal.ZERO, BoqItem::getTotalAmount, BigDecimal::add)));

        List<Map<String, Object>> breakdown = workTypeTotals.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("workType", e.getKey());
                    entry.put("total", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("projectId", projectId);
        summary.put("totalItems", items.size());
        summary.put("totalAmount", totalAmount);
        summary.put("workTypeBreakdown", breakdown);

        return summary;
    }
}
