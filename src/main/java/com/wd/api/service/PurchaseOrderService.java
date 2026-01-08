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
            po.setStatus(com.wd.api.model.enums.PurchaseOrderStatus.DRAFT);

        // Generate PO Number if missing
        if (po.getPoNumber() == null) {
            po.setPoNumber("PO-" + System.currentTimeMillis());
        }

        return poRepository.save(po);
    }

    private void validateItemBudget(CustomerProject project, PurchaseOrderItem item) {
        // TODO: DESIGN GAP - BoqItem does not have material relationship
        // This logic assumes BoqItem tracks individual materials, but BoqItem is
        // work-type based
        // Proper solution: Either add Material → BoqItem relationship OR
        // implement a separate Material Budget table

        // Budget validation temporarily disabled - needs architectural redesign
        // For enterprise implementation: Create MaterialBudget entity linking Material
        // → Project → Budget
    }

    public List<PurchaseOrder> getProjectPurchaseOrders(Long projectId) {
        if (projectId == null)
            return List.of();
        return poRepository.findByProjectId(projectId);
    }
}
