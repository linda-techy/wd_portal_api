package com.wd.api.controller;

import com.wd.api.dto.AccountsPayableAgingDTO;
import com.wd.api.dto.VendorOutstandingDTO;
import com.wd.api.model.PurchaseInvoice;
import com.wd.api.model.VendorPayment;
import com.wd.api.service.VendorPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Accounts Payable Controller
 * Handles vendor payments and AP reporting
 */
@RestController
@RequestMapping("/api/accounts-payable")
@RequiredArgsConstructor
// CORS configuration is handled globally in SecurityConfig
public class AccountsPayableController {

    private final VendorPaymentService vendorPaymentService;

    // ===== PAYMENT ENDPOINTS =====

    /**
     * Record a vendor payment
     */
    @PostMapping("/payments")
    public ResponseEntity<VendorPayment> recordPayment(@RequestBody VendorPayment payment) {
        VendorPayment recorded = vendorPaymentService.recordPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    /**
     * Get payment history for an invoice
     */
    @GetMapping("/invoices/{invoiceId}/payments")
    public ResponseEntity<List<VendorPayment>> getInvoicePayments(@PathVariable Long invoiceId) {
        List<VendorPayment> payments = vendorPaymentService.getPaymentHistory(invoiceId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments for a vendor
     */
    @GetMapping("/vendors/{vendorId}/payments")
    public ResponseEntity<List<VendorPayment>> getVendorPayments(@PathVariable Long vendorId) {
        List<VendorPayment> payments = vendorPaymentService.getVendorPayments(vendorId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payments by date range
     */
    @GetMapping("/payments")
    public ResponseEntity<List<VendorPayment>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<VendorPayment> payments = vendorPaymentService.getPaymentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(payments);
    }

    // ===== ACCOUNTS PAYABLE AGING =====

    /**
     * Get accounts payable aging report
     * Returns vendor-wise aging buckets (0-30, 31-60, >60 days)
     */
    @GetMapping("/aging")
    public ResponseEntity<AccountsPayableAgingDTO> getAccountsPayableAging() {
        AccountsPayableAgingDTO aging = vendorPaymentService.getAccountsPayableAging();
        return ResponseEntity.ok(aging);
    }

    /**
     * Get vendor outstanding summary
     */
    @GetMapping("/vendor-outstanding")
    public ResponseEntity<List<VendorOutstandingDTO>> getVendorOutstanding() {
        List<VendorOutstandingDTO> outstanding = vendorPaymentService.getVendorOutstandingSummaries();
        return ResponseEntity.ok(outstanding);
    }

    /**
     * Get outstanding for specific vendor
     */
    @GetMapping("/vendors/{vendorId}/outstanding")
    public ResponseEntity<VendorOutstandingDTO> getVendorOutstandingDetail(@PathVariable Long vendorId) {
        VendorOutstandingDTO outstanding = vendorPaymentService.getVendorOutstanding(vendorId);
        return ResponseEntity.ok(outstanding);
    }

    // ===== PENDING & OVERDUE INVOICES =====

    /**
     * Get all pending invoices (UNPAID or PARTIAL)
     */
    @GetMapping("/invoices/pending")
    public ResponseEntity<List<PurchaseInvoice>> getPendingInvoices() {
        List<PurchaseInvoice> pending = vendorPaymentService.getPendingInvoices();
        return ResponseEntity.ok(pending);
    }

    /**
     * Get overdue invoices
     */
    @GetMapping("/invoices/overdue")
    public ResponseEntity<List<PurchaseInvoice>> getOverdueInvoices() {
        List<PurchaseInvoice> overdue = vendorPaymentService.getOverdueInvoices();
        return ResponseEntity.ok(overdue);
    }

    // ===== STATISTICS =====

    /**
     * Get total paid amount in a period
     */
    @GetMapping("/statistics/total-paid")
    public ResponseEntity<BigDecimal> getTotalPaidInPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal totalPaid = vendorPaymentService.getTotalPaidInPeriod(startDate, endDate);
        return ResponseEntity.ok(totalPaid);
    }

    /**
     * Get total TDS deducted in a period
     */
    @GetMapping("/statistics/total-tds")
    public ResponseEntity<BigDecimal> getTotalTdsDeducted(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal totalTds = vendorPaymentService.getTotalTdsDeducted(startDate, endDate);
        return ResponseEntity.ok(totalTds);
    }

    // ===== EXCEPTION HANDLING =====

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
