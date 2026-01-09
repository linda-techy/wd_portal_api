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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                .map(this::mapToVendorDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrderDTO createPurchaseOrder(PurchaseOrderDTO dto) {
        Long vendorId = dto.getVendorId();
        Long projectId = dto.getProjectId();
        if (vendorId == null || projectId == null) {
            throw new IllegalArgumentException("Vendor ID and Project ID are required");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

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

    private String generatePONumber() {
        return "WAL/PO/" + java.time.LocalDate.now().getYear() + "/" + System.currentTimeMillis() % 10000;
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
