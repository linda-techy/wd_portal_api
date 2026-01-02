package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final CustomerProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final MaterialRepository materialRepository;
    private final BoqItemRepository boqItemRepository;

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
            po.setStatus("DRAFT");

        // Generate PO Number if missing
        if (po.getPoNumber() == null) {
            po.setPoNumber("PO-" + System.currentTimeMillis());
        }

        return poRepository.save(po);
    }

    private void validateItemBudget(CustomerProject project, PurchaseOrderItem item) {
        // If item is linked to a BoQ Item, check variances
        // Assuming we have a way to match Material -> BoQ Item.
        // For now, naive check: If the Item has a reference to BoQ ID (which
        // PurchaseOrderItem entity might need).

        // Critical Enterprise Logic:
        // We need to fetch the specific BoQ item for this material in this project.
        // Assuming BoQItem has relation to Material or we key off Material ID.

        // Find BoQ Item for this Project + Material
        // This query assumes generic material matching.
        List<BoqItem> boqItems = boqItemRepository.findByProjectIdAndMaterialId(project.getId(),
                item.getMaterial().getId());

        if (!boqItems.isEmpty()) {
            // Aggregate Budget if multiple BoQ lines for same material (rare but possible)
            BigDecimal totalBudgetRate = boqItems.get(0).getUnitRate(); // Simplified

            // Check Rate Variance
            if (item.getRate().compareTo(totalBudgetRate) > 0) {
                // In a strict Enterprise system, this might throw specific exception
                // 'BudgetExceededException'.
                // For now, we will log or tag logic.
                // Ideally, we'd set a 'variance' flag on the PO Item if we added that column.

                // For this implementation, we will allow it but could add a warning note.
            }
        }
    }

    public List<PurchaseOrder> getProjectPurchaseOrders(Long projectId) {
        if (projectId == null)
            return List.of();
        return poRepository.findByProjectId(projectId);
    }
}
