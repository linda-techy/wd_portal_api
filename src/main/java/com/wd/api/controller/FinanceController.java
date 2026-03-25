package com.wd.api.controller;

import com.wd.api.dto.LabourPaymentDTO;
import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.dto.PurchaseInvoiceDTO;
import com.wd.api.model.DesignPackagePayment;
import com.wd.api.service.FinanceService;
import com.wd.api.service.RetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final RetentionService retentionService;

    @PostMapping("/invoice/create")
    public ResponseEntity<ProjectInvoiceDTO> createProjectInvoice(@RequestBody ProjectInvoiceDTO dto) {
        return ResponseEntity.ok(financeService.createProjectInvoice(dto));
    }

    @PostMapping("/purchase-invoice/record")
    public ResponseEntity<PurchaseInvoiceDTO> recordPurchaseInvoice(@RequestBody PurchaseInvoiceDTO dto) {
        return ResponseEntity.ok(financeService.recordPurchaseInvoice(dto));
    }

    @PostMapping("/labour-payment/record")
    public ResponseEntity<LabourPaymentDTO> recordLabourPayment(@RequestBody LabourPaymentDTO dto) {
        return ResponseEntity.ok(financeService.recordLabourPayment(dto));
    }

    @GetMapping("/invoices/project/{projectId}")
    public ResponseEntity<List<ProjectInvoiceDTO>> getInvoicesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(financeService.getInvoicesByProject(projectId));
    }

    // Milestones
    @PostMapping("/milestone")
    public ResponseEntity<com.wd.api.model.ProjectMilestone> createMilestone(
            @RequestBody com.wd.api.model.ProjectMilestone milestone) {
        return ResponseEntity.ok(financeService.createMilestone(milestone));
    }

    @PutMapping("/milestone/{id}")
    public ResponseEntity<com.wd.api.model.ProjectMilestone> updateMilestone(@PathVariable Long id,
            @RequestBody com.wd.api.model.ProjectMilestone milestone) {
        return ResponseEntity.ok(financeService.updateMilestone(id, milestone));
    }

    @PostMapping("/milestone/{id}/generate-invoice")
    public ResponseEntity<ProjectInvoiceDTO> generateInvoiceForMilestone(@PathVariable Long id) {
        return ResponseEntity.ok(financeService.generateInvoiceForMilestone(id));
    }

    @GetMapping("/milestones/project/{projectId}")
    public ResponseEntity<List<com.wd.api.model.ProjectMilestone>> getMilestonesByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(financeService.getMilestonesByProject(projectId));
    }

    // Receipts
    @PostMapping("/receipt")
    public ResponseEntity<com.wd.api.model.Receipt> recordReceipt(@RequestBody com.wd.api.model.Receipt receipt) {
        return ResponseEntity.ok(financeService.recordReceipt(receipt));
    }

    @GetMapping("/receipts/project/{projectId}")
    public ResponseEntity<List<com.wd.api.model.Receipt>> getReceiptsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(financeService.getReceiptsByProject(projectId));
    }

    // Payment Reconciliation
    @PostMapping("/transactions/{transactionId}/reconcile/{invoiceId}")
    public ResponseEntity<ProjectInvoiceDTO> reconcilePayment(
            @PathVariable Long transactionId,
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(financeService.reconcilePayment(transactionId, invoiceId));
    }

    // Retention Release
    @PostMapping("/retention/{designPaymentId}/release")
    @PreAuthorize("hasAnyAuthority('FINANCE_MANAGE', 'ADMIN')")
    public ResponseEntity<DesignPackagePayment> releaseRetention(@PathVariable Long designPaymentId) {
        return ResponseEntity.ok(retentionService.releaseRetention(designPaymentId));
    }
}
