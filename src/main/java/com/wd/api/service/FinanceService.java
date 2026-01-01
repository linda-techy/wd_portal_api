package com.wd.api.service;

import com.wd.api.dto.LabourPaymentDTO;
import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.dto.PurchaseInvoiceDTO;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final ProjectInvoiceRepository projectInvoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final LabourPaymentRepository labourPaymentRepository;
    private final CustomerProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final LabourRepository labourRepository;
    private final MeasurementBookRepository mbRepository;
    private final PurchaseOrderRepository poRepository;
    private final GoodsReceivedNoteRepository grnRepository;

    @Transactional
    public ProjectInvoiceDTO createProjectInvoice(ProjectInvoiceDTO dto) {
        CustomerProject project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        BigDecimal gstRate = dto.getGstPercentage() != null ? dto.getGstPercentage() : new BigDecimal("18.00");
        BigDecimal gstAmount = dto.getSubTotal().multiply(gstRate).divide(new BigDecimal("100"), 2,
                RoundingMode.HALF_UP);
        BigDecimal totalAmount = dto.getSubTotal().add(gstAmount);

        ProjectInvoice invoice = ProjectInvoice.builder()
                .project(project)
                .invoiceNumber(generateInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : java.time.LocalDate.now())
                .dueDate(dto.getDueDate())
                .subTotal(dto.getSubTotal())
                .gstPercentage(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .status("ISSUED")
                .notes(dto.getNotes())
                .build();

        invoice = projectInvoiceRepository.save(invoice);
        return mapToProjectInvoiceDTO(invoice);
    }

    @Transactional
    public PurchaseInvoiceDTO recordPurchaseInvoice(PurchaseInvoiceDTO dto) {
        Vendor vendor = vendorRepository.findById(dto.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        CustomerProject project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        PurchaseOrder po = dto.getPoId() != null ? poRepository.findById(dto.getPoId()).orElse(null) : null;
        GoodsReceivedNote grn = dto.getGrnId() != null ? grnRepository.findById(dto.getGrnId()).orElse(null) : null;

        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .vendor(vendor)
                .project(project)
                .purchaseOrder(po)
                .grn(grn)
                .vendorInvoiceNumber(dto.getVendorInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate())
                .amount(dto.getAmount())
                .status("PENDING")
                .build();

        invoice = purchaseInvoiceRepository.save(invoice);
        return mapToPurchaseInvoiceDTO(invoice);
    }

    @Transactional
    public LabourPaymentDTO recordLabourPayment(LabourPaymentDTO dto) {
        Labour labour = labourRepository.findById(dto.getLabourId())
                .orElseThrow(() -> new RuntimeException("Labour not found"));
        CustomerProject project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        MeasurementBook mbEntry = dto.getMbEntryId() != null ? mbRepository.findById(dto.getMbEntryId()).orElse(null)
                : null;

        LabourPayment payment = LabourPayment.builder()
                .labour(labour)
                .project(project)
                .mbEntry(mbEntry)
                .amount(dto.getAmount())
                .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : java.time.LocalDate.now())
                .paymentMethod(dto.getPaymentMethod())
                .notes(dto.getNotes())
                .build();

        payment = labourPaymentRepository.save(payment);
        return mapToLabourPaymentDTO(payment);
    }

    public List<ProjectInvoiceDTO> getInvoicesByProject(Long projectId) {
        return projectInvoiceRepository.findByProjectId(projectId).stream()
                .map(this::mapToProjectInvoiceDTO)
                .collect(Collectors.toList());
    }

    private String generateInvoiceNumber() {
        return "WAL/INV/" + java.time.LocalDate.now().getYear() + "/" + System.currentTimeMillis() % 10000;
    }

    private ProjectInvoiceDTO mapToProjectInvoiceDTO(ProjectInvoice inv) {
        return ProjectInvoiceDTO.builder()
                .id(inv.getId())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .invoiceNumber(inv.getInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(inv.getDueDate())
                .subTotal(inv.getSubTotal())
                .gstPercentage(inv.getGstPercentage())
                .gstAmount(inv.getGstAmount())
                .totalAmount(inv.getTotalAmount())
                .status(inv.getStatus())
                .notes(inv.getNotes())
                .build();
    }

    private PurchaseInvoiceDTO mapToPurchaseInvoiceDTO(PurchaseInvoice inv) {
        return PurchaseInvoiceDTO.builder()
                .id(inv.getId())
                .vendorId(inv.getVendor().getId())
                .vendorName(inv.getVendor().getName())
                .projectId(inv.getProject().getId())
                .projectName(inv.getProject().getName())
                .poId(inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getId() : null)
                .grnId(inv.getGrn() != null ? inv.getGrn().getId() : null)
                .vendorInvoiceNumber(inv.getVendorInvoiceNumber())
                .invoiceDate(inv.getInvoiceDate())
                .amount(inv.getAmount())
                .status(inv.getStatus())
                .build();
    }

    private LabourPaymentDTO mapToLabourPaymentDTO(LabourPayment p) {
        return LabourPaymentDTO.builder()
                .id(p.getId())
                .labourId(p.getLabour().getId())
                .labourName(p.getLabour().getName())
                .projectId(p.getProject().getId())
                .projectName(p.getProject().getName())
                .mbEntryId(p.getMbEntry() != null ? p.getMbEntry().getId() : null)
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .notes(p.getNotes())
                .build();
    }
}
