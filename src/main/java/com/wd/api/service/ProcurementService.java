package com.wd.api.service;

import com.wd.api.dto.GRNDTO;
import com.wd.api.dto.VendorDTO;
import com.wd.api.dto.PurchaseOrderDTO;
import com.wd.api.model.Vendor;
import com.wd.api.model.PurchaseOrder;
import com.wd.api.model.PurchaseOrderItem;
import com.wd.api.model.GoodsReceivedNote;
import com.wd.api.model.enums.PurchaseOrderStatus;
import com.wd.api.repository.VendorRepository;
import com.wd.api.repository.PurchaseOrderRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.GoodsReceivedNoteRepository;
import com.wd.api.repository.MaterialRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository poRepository;
    private final CustomerProjectRepository projectRepository;

    private final GoodsReceivedNoteRepository grnRepository;
    private final InventoryService inventoryService;
    private final MaterialRepository materialRepository;

    @Transactional
    public VendorDTO createVendor(VendorDTO dto) {
        Vendor vendor = Vendor.builder()
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .gstin(dto.getGstin())
                .address(dto.getAddress())
                .vendorType(dto.getVendorType())
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .ifscCode(dto.getIfscCode())
                .active(true)
                .build();
        Vendor savedVendor = java.util.Objects.requireNonNull(vendorRepository.save(vendor));
        return mapToVendorDTO(savedVendor);
    }

    public List<VendorDTO> getAllVendors() {
        return vendorRepository.findAll().stream()
                .filter(vendor -> vendor.isActive()) // Only return active vendors
                .map(this::mapToVendorDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update Vendor (construction workflow - update contact/bank info)
     */
    @Transactional
    public VendorDTO updateVendor(Long id, VendorDTO dto) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + id));

        // Update fields
        if (dto.getName() != null)
            vendor.setName(dto.getName());
        if (dto.getContactPerson() != null)
            vendor.setContactPerson(dto.getContactPerson());
        if (dto.getPhone() != null)
            vendor.setPhone(dto.getPhone());
        if (dto.getEmail() != null)
            vendor.setEmail(dto.getEmail());
        if (dto.getGstin() != null)
            vendor.setGstin(dto.getGstin());
        if (dto.getAddress() != null)
            vendor.setAddress(dto.getAddress());
        if (dto.getVendorType() != null)
            vendor.setVendorType(dto.getVendorType());
        if (dto.getBankName() != null)
            vendor.setBankName(dto.getBankName());
        if (dto.getAccountNumber() != null)
            vendor.setAccountNumber(dto.getAccountNumber());
        if (dto.getIfscCode() != null)
            vendor.setIfscCode(dto.getIfscCode());

        Vendor saved = vendorRepository.save(vendor);
        log.info("Updated Vendor ID: {}", id);
        return mapToVendorDTO(saved);
    }

    /**
     * Deactivate Vendor (construction best practice - never hard delete)
     * Vendors linked to POs cannot be deleted, only deactivated
     */
    @Transactional
    public void deactivateVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + id));

        // Check for active POs - don't allow deactivation if vendor has active POs
        List<PurchaseOrder> activePOs = poRepository.findByVendorId(id).stream()
                .filter(po -> po.getStatus() != PurchaseOrderStatus.CLOSED &&
                        po.getStatus() != PurchaseOrderStatus.CANCELLED)
                .collect(Collectors.toList());

        if (!activePOs.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot deactivate vendor with " + activePOs.size() + " active Purchase Orders. " +
                            "Close or cancel all POs first.");
        }

        vendor.setActive(false);
        vendorRepository.save(vendor);
        log.info("Deactivated Vendor ID: {}", id);
    }

    @Transactional
    public PurchaseOrderDTO createPurchaseOrder(PurchaseOrderDTO dto) {
        Long vendorId = dto.getVendorId();
        Long projectId = dto.getProjectId();
        if (vendorId == null || projectId == null) {
            log.error("PO Creation Failed: Vendor ID or Project ID missing. Payload: {}", dto);
            throw new IllegalArgumentException("Vendor ID and Project ID are required");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found with ID: " + vendorId));
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

        log.info("Creating Purchase Order - Project: {}, Vendor: {}", project.getName(), vendor.getName());

        // Use setters since PurchaseOrder doesn't have Lombok @Builder
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(generatePONumber());
        po.setVendor(vendor);
        po.setProject(project);
        po.setPoDate(dto.getPoDate() != null ? dto.getPoDate() : java.time.LocalDate.now());
        po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        po.setTotalAmount(dto.getTotalAmount());
        po.setGstAmount(dto.getGstAmount());
        po.setNetAmount(dto.getNetAmount());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setNotes(dto.getNotes());

        if (dto.getItems() != null) {
            List<PurchaseOrderItem> items = dto.getItems().stream()
                    .map(itemDto -> {
                        PurchaseOrderItem item = new PurchaseOrderItem();
                        item.setDescription(itemDto.getDescription());
                        item.setQuantity(itemDto.getQuantity());
                        item.setUnit(itemDto.getUnit());
                        item.setRate(itemDto.getRate());
                        item.setGstPercentage(itemDto.getGstPercentage());
                        item.setAmount(itemDto.getAmount());
                        Long matId = itemDto.getMaterialId();
                        item.setMaterial(matId != null
                                ? materialRepository.findById(matId).orElse(null)
                                : null);
                        return item;
                    })
                    .collect(Collectors.toList());
            po.setItems(items);
            items.forEach(item -> item.setPurchaseOrder(po));
        }

        PurchaseOrder savedPo = poRepository.save(po);
        return mapToPODTO(savedPo);
    }

    @Transactional
    public GRNDTO recordGRN(GRNDTO dto) {
        Long poId = dto.getPoId();
        if (poId == null) {
            throw new IllegalArgumentException("Purchase Order ID is required");
        }
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        GoodsReceivedNote grn = GoodsReceivedNote.builder()
                .grnNumber(generateGRNNumber())
                .purchaseOrder(po)
                .receivedDate(java.time.LocalDateTime.now())
                .receivedById(dto.getReceivedById())
                .invoiceNumber(dto.getInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate())
                .challanNumber(dto.getChallanNumber())
                .notes(dto.getNotes())
                .build();

        grnRepository.save(grn);

        // Update PO status using proper enum
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        poRepository.save(po);

        // Update Inventory Stock
        if (po.getItems() != null) {
            for (PurchaseOrderItem item : po.getItems()) {
                if (item.getMaterial() != null) {
                    inventoryService.updateStock(po.getProject().getId(), item.getMaterial().getId(),
                            item.getQuantity());
                }
            }
        }

        return mapToGRNDTO(grn);
    }

    /**
     * Search Purchase Orders with pagination and filters (Enterprise-grade)
     * Excludes soft-deleted records automatically
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> searchPurchaseOrders(String searchTerm, String status, Long projectId,
            Pageable pageable) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted records
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Search filter (PO Number, Vendor Name, or Project Name)
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("poNumber")), searchPattern),
                        cb.like(cb.lower(root.get("vendor").get("name")), searchPattern),
                        cb.like(cb.lower(root.get("project").get("name")), searchPattern)));
            }

            // Status filter
            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                try {
                    predicates.add(cb.equal(root.get("status"), PurchaseOrderStatus.valueOf(status)));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", status);
                }
            }

            // Project filter
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return poRepository.findAll(spec, pageable).map(this::mapToPODTO);
    }

    /**
     * Soft delete Purchase Order (Enterprise-grade - preserves audit trail)
     */
    @Transactional
    public void softDeletePurchaseOrder(Long id, Long deletedByUserId) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        // Validation: Cannot delete if already received
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException(
                    "Cannot delete Purchase Order that has already been RECEIVED. Please reverse the GRN first.");
        }

        // Validation: Cannot delete if already deleted
        if (po.isDeleted()) {
            throw new IllegalStateException("Purchase Order is already deleted.");
        }

        // Soft delete
        po.setDeletedAt(LocalDateTime.now());
        po.setDeletedByUserId(deletedByUserId);
        poRepository.save(po);

        log.info("Soft-deleted Purchase Order ID: {} by User ID: {}", id, deletedByUserId);
    }

    /**
     * Cancel Purchase Order (Enterprise-grade - better than delete)
     */
    @Transactional
    public PurchaseOrderDTO cancelPurchaseOrder(Long id, String cancelReason) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        // Validation: Can only cancel DRAFT or ISSUED POs
        if (po.getStatus() != PurchaseOrderStatus.DRAFT && po.getStatus() != PurchaseOrderStatus.ISSUED) {
            throw new IllegalStateException(
                    "Can only cancel Purchase Orders in DRAFT or ISSUED status. Current status: " + po.getStatus());
        }

        // Validate status transition
        validateStatusTransition(po.getStatus(), PurchaseOrderStatus.CANCELLED);

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        if (cancelReason != null && !cancelReason.trim().isEmpty()) {
            String updatedNotes = (po.getNotes() != null ? po.getNotes() + "\n\n" : "")
                    + "CANCELLED: " + cancelReason;
            po.setNotes(updatedNotes);
        }

        PurchaseOrder saved = poRepository.save(po);
        log.info("Cancelled Purchase Order ID: {}. Reason: {}", id, cancelReason);
        return mapToPODTO(saved);
    }

    /**
     * Close Purchase Order (Construction workflow - after all goods received and
     * verified)
     */
    @Transactional
    public PurchaseOrderDTO closePurchaseOrder(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        // Validation: Can only close RECEIVED POs
        if (po.getStatus() != PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException(
                    "Can only close Purchase Orders in RECEIVED status. Current status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.CLOSED);
        String updatedNotes = (po.getNotes() != null ? po.getNotes() + "\n\n" : "")
                + "CLOSED: " + java.time.LocalDateTime.now();
        po.setNotes(updatedNotes);

        PurchaseOrder saved = poRepository.save(po);
        log.info("Closed Purchase Order ID: {}", id);
        return mapToPODTO(saved);
    }

    /**
     * Update Purchase Order (Enterprise-grade with validation)
     */
    @Transactional
    public PurchaseOrderDTO updatePurchaseOrder(Long id, PurchaseOrderDTO dto) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with ID: " + id));

        // Validation: Cannot edit if already received or cancelled
        if (!po.isEditable()) {
            throw new IllegalStateException("Cannot edit Purchase Order with status: " + po.getStatus());
        }

        // Update fields
        if (dto.getNotes() != null) {
            po.setNotes(dto.getNotes());
        }
        if (dto.getExpectedDeliveryDate() != null) {
            po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }

        // Status update with validation
        if (dto.getStatus() != null && !dto.getStatus().equals(po.getStatus().name())) {
            try {
                PurchaseOrderStatus newStatus = PurchaseOrderStatus.valueOf(dto.getStatus());
                validateStatusTransition(po.getStatus(), newStatus);
                po.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + dto.getStatus());
            }
        }

        PurchaseOrder saved = poRepository.save(po);
        log.info("Updated Purchase Order ID: {}", id);
        return mapToPODTO(saved);
    }

    /**
     * Validate Purchase Order status transitions (Enterprise business rules)
     */
    private void validateStatusTransition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        // Define allowed transitions
        boolean isValid = false;

        switch (from) {
            case DRAFT:
                isValid = (to == PurchaseOrderStatus.ISSUED || to == PurchaseOrderStatus.CANCELLED);
                break;
            case ISSUED:
                isValid = (to == PurchaseOrderStatus.PARTIALLY_RECEIVED ||
                        to == PurchaseOrderStatus.RECEIVED ||
                        to == PurchaseOrderStatus.CANCELLED);
                break;
            case PARTIALLY_RECEIVED:
                isValid = (to == PurchaseOrderStatus.RECEIVED);
                break;
            case RECEIVED:
            case CANCELLED:
            case CLOSED:
                // Terminal states - no transitions allowed
                isValid = false;
                break;
        }

        if (!isValid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    /**
     * Get all GRNs (Goods Received Notes) for centralized list view
     * Enterprise feature - shows all receipts across projects
     */
    public List<GRNDTO> getAllGRNs() {
        return grnRepository.findAll().stream()
                .map(this::mapToGRNDTOWithExtras)
                .collect(Collectors.toList());
    }

    /**
     * Enhanced GRN mapper with vendor and project info for list view
     */
    private GRNDTO mapToGRNDTOWithExtras(GoodsReceivedNote g) {
        PurchaseOrder po = g.getPurchaseOrder();
        GRNDTO dto = GRNDTO.builder()
                .id(g.getId())
                .grnNumber(g.getGrnNumber())
                .poId(po.getId())
                .poNumber(po.getPoNumber())
                .receivedDate(g.getReceivedDate())
                .receivedById(g.getReceivedById())
                .invoiceNumber(g.getInvoiceNumber())
                .invoiceDate(g.getInvoiceDate())
                .challanNumber(g.getChallanNumber())
                .notes(g.getNotes())
                .build();
        // Add vendor and project info
        dto.setVendorName(po.getVendor() != null ? po.getVendor().getName() : null);
        dto.setProjectName(po.getProject() != null ? po.getProject().getName() : null);
        return dto;
    }

    private String generatePONumber() {
        // Format: PO-YYYYMMDD-HHMMSS-RAND3
        return "PO-"
                + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now())
                + "-" + (int) (Math.random() * 1000);
    }

    private String generateGRNNumber() {
        return "WAL/GRN/" + java.time.LocalDate.now().getYear() + "/" + System.currentTimeMillis() % 10000;
    }

    private VendorDTO mapToVendorDTO(Vendor v) {
        VendorDTO dto = new VendorDTO();
        dto.setId(v.getId());
        dto.setName(v.getName());
        dto.setContactPerson(v.getContactPerson());
        dto.setPhone(v.getPhone());
        dto.setEmail(v.getEmail());
        dto.setGstin(v.getGstin());
        dto.setAddress(v.getAddress());
        dto.setVendorType(v.getVendorType());
        dto.setBankName(v.getBankName());
        dto.setAccountNumber(v.getAccountNumber());
        dto.setIfscCode(v.getIfscCode());
        dto.setActive(v.isActive());
        dto.setCreatedAt(v.getCreatedAt());
        dto.setUpdatedAt(v.getUpdatedAt());
        return dto;
    }

    private PurchaseOrderDTO mapToPODTO(PurchaseOrder p) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setId(p.getId());
        dto.setPoNumber(p.getPoNumber());
        dto.setProjectId(p.getProject().getId());
        dto.setProjectName(p.getProject().getName());
        dto.setVendorId(p.getVendor().getId());
        dto.setVendorName(p.getVendor().getName());
        dto.setPoDate(p.getPoDate());
        dto.setStatus(p.getStatus().name());
        dto.setTotalAmount(p.getTotalAmount());
        dto.setGstAmount(p.getGstAmount());
        dto.setNetAmount(p.getNetAmount());
        return dto;
    }

    private com.wd.api.dto.GRNDTO mapToGRNDTO(com.wd.api.model.GoodsReceivedNote g) {
        com.wd.api.dto.GRNDTO dto = new com.wd.api.dto.GRNDTO();
        dto.setId(g.getId());
        dto.setGrnNumber(g.getGrnNumber());
        dto.setPoId(g.getPurchaseOrder().getId());
        dto.setPoNumber(g.getPurchaseOrder().getPoNumber());
        dto.setReceivedDate(g.getReceivedDate());
        dto.setReceivedById(g.getReceivedById());
        dto.setInvoiceNumber(g.getInvoiceNumber());
        return dto;
    }
}
