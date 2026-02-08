package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import com.wd.api.dto.PurchaseOrderSearchFilter;
import com.wd.api.util.SpecificationBuilder;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final CustomerProjectRepository projectRepository;
    private final VendorRepository vendorRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(Long projectId, Long vendorId, PurchaseOrder po) {
        if (projectId == null || vendorId == null)
            throw new IllegalArgumentException("Project ID and Vendor ID cannot be null");
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Validate Budget for each Item
        if (po.getItems() != null) {
            for (PurchaseOrderItem item : po.getItems()) {
                validateItemBudget(project, item);
                item.setPurchaseOrder(po);
            }
        }

        po.setProject(project);
        po.setVendor(vendor);
        po.setCreatedAt(LocalDateTime.now());
        if (po.getStatus() == null)
            po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.DRAFT);

        // Generate PO Number if missing
        if (po.getPoNumber() == null) {
            po.setPoNumber("PO-" + System.currentTimeMillis());
        }

        return poRepository.save(po);
    }

    private void validateItemBudget(CustomerProject project, PurchaseOrderItem item) {
        // This logic assumes BoqItem tracks individual materials, but BoqItem is
        // work-type based
        // Proper solution: Either add Material → BoqItem relationship OR
        // implement a separate Material Budget table

        // Budget validation temporarily disabled - needs architectural redesign
        // For enterprise implementation: Create MaterialBudget entity linking Material
        // → Project → Budget
    }

    /**
     * NEW: Standardized search method using PurchaseOrderSearchFilter
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Page<PurchaseOrder> search(PurchaseOrderSearchFilter filter) {
        Specification<PurchaseOrder> spec = buildSearchSpecification(filter);
        return poRepository.findAll(spec, filter.toPageable());
    }

    /**
     * Build JPA Specification from PurchaseOrderSearchFilter
     */
    private Specification<PurchaseOrder> buildSearchSpecification(PurchaseOrderSearchFilter filter) {
        SpecificationBuilder<PurchaseOrder> builder = new SpecificationBuilder<>();

        // Search across multiple fields
        Specification<PurchaseOrder> searchSpec = builder.buildSearch(
                filter.getSearchQuery(),
                "poNumber", "description", "notes");

        // Apply filters
        Specification<PurchaseOrder> statusSpec = null;
        if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
            statusSpec = (root, query, cb) -> cb.equal(root.get("status"),
                    com.wd.api.model.enums.PurchaseOrderStatus.valueOf(filter.getStatus().toUpperCase()));
        }

        // Vendor filter
        Specification<PurchaseOrder> vendorSpec = null;
        if (filter.getVendorId() != null) {
            vendorSpec = (root, query, cb) -> cb.equal(root.get("vendor").get("id"), filter.getVendorId());
        }

        // Project filter
        Specification<PurchaseOrder> projectSpec = null;
        if (filter.getProjectId() != null) {
            projectSpec = (root, query, cb) -> cb.equal(root.get("project").get("id"), filter.getProjectId());
        }

        // PO Number filter (partial match)
        Specification<PurchaseOrder> poNumberSpec = builder.buildLike("poNumber", filter.getPoNumber());

        // Amount range
        Specification<PurchaseOrder> amountSpec = builder.buildNumericRange(
                "totalAmount",
                filter.getMinAmount(),
                filter.getMaxAmount());

        // Date range (on createdAt)
        Specification<PurchaseOrder> dateRangeSpec = null;
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

        // Combine all specifications
        return builder.and(
                searchSpec,
                statusSpec,
                vendorSpec,
                projectSpec,
                poNumberSpec,
                amountSpec,
                dateRangeSpec);
    }

    /**
     * DEPRECATED: Use search() instead
     */
    @Deprecated
    public List<PurchaseOrder> getProjectPurchaseOrders(Long projectId) {
        if (projectId == null)
            return List.of();
        return poRepository.findByProjectId(projectId);
    }
}
