package com.wd.api.controller;

import com.wd.api.dto.LabourPaymentDTO;
import com.wd.api.dto.ProjectInvoiceDTO;
import com.wd.api.dto.PurchaseInvoiceDTO;
import com.wd.api.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

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
}
