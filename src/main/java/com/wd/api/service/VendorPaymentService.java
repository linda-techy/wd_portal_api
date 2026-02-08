package com.wd.api.service;

import com.wd.api.dto.AccountsPayableAgingDTO;
import com.wd.api.dto.VendorOutstandingDTO;
import com.wd.api.model.PurchaseInvoice;
import com.wd.api.model.Vendor;
import com.wd.api.model.VendorPayment;
import com.wd.api.repository.PurchaseInvoiceRepository;
import com.wd.api.repository.VendorPaymentRepository;
import com.wd.api.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Vendor Payment Service
 * Business logic for vendor payment tracking and accounts payable
 */
@Service
@RequiredArgsConstructor
public class VendorPaymentService {

    private final VendorPaymentRepository vendorPaymentRepo;
    private final PurchaseInvoiceRepository invoiceRepo;
    private final VendorRepository vendorRepo;
    private final JdbcTemplate jdbcTemplate;

    // ===== PAYMENT OPERATIONS =====

    @Transactional
    public VendorPayment recordPayment(VendorPayment payment) {
        // Auto-calculate net paid
        payment.calculateNetPaid();

        VendorPayment saved = vendorPaymentRepo.save(payment);

        // Trigger will automatically update invoice paid_amount and status

        return saved;
    }

    public List<VendorPayment> getPaymentHistory(Long invoiceId) {
        return vendorPaymentRepo.findByInvoiceId(invoiceId);
    }

    public List<VendorPayment> getVendorPayments(Long vendorId) {
        return vendorPaymentRepo.findByVendorId(vendorId);
    }

    public List<VendorPayment> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return vendorPaymentRepo.findByPaymentDateBetween(startDate, endDate);
    }

    // ===== ACCOUNTS PAYABLE AGING =====

    public AccountsPayableAgingDTO getAccountsPayableAging() {
        // Use the database view for efficient calculation
        String sql = "SELECT * FROM v_accounts_payable_aging";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        AccountsPayableAgingDTO aging = new AccountsPayableAgingDTO();
        aging.setVendorBreakdown(new ArrayList<>());

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal total_0_30 = BigDecimal.ZERO;
        BigDecimal total_31_60 = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        int totalInvoices = 0;
        int totalOverdueCount = 0;

        for (Map<String, Object> row : rows) {
            AccountsPayableAgingDTO.VendorAgingDetail detail = new AccountsPayableAgingDTO.VendorAgingDetail();

            detail.setVendorId(((Number) row.get("vendor_id")).longValue());
            detail.setVendorName((String) row.get("vendor_name"));
            detail.setInvoiceCount(((Number) row.get("total_invoices")).intValue());

            BigDecimal vendorOutstanding = (BigDecimal) row.get("total_outstanding");
            BigDecimal vendor_0_30 = (BigDecimal) row.get("due_0_30_days");
            BigDecimal vendor_31_60 = (BigDecimal) row.get("due_31_60_days");
            BigDecimal vendorOverdue = (BigDecimal) row.get("overdue");

            detail.setTotalOutstanding(vendorOutstanding);
            detail.setDue_0_30_days(vendor_0_30);
            detail.setDue_31_60_days(vendor_31_60);
            detail.setOverdue(vendorOverdue);
            detail.setOverdueInvoiceCount(((Number) row.get("overdue_invoice_count")).intValue());

            aging.getVendorBreakdown().add(detail);

            totalOutstanding = totalOutstanding.add(vendorOutstanding);
            total_0_30 = total_0_30.add(vendor_0_30);
            total_31_60 = total_31_60.add(vendor_31_60);
            totalOverdue = totalOverdue.add(vendorOverdue);
            totalInvoices += detail.getInvoiceCount();
            totalOverdueCount += detail.getOverdueInvoiceCount();
        }

        aging.setTotalOutstanding(totalOutstanding);
        aging.setDue_0_30_days(total_0_30);
        aging.setDue_31_60_days(total_31_60);
        aging.setOverdue(totalOverdue);
        aging.setTotalVendors(rows.size());
        aging.setTotalInvoices(totalInvoices);
        aging.setOverdueInvoiceCount(totalOverdueCount);

        return aging;
    }

    // ===== VENDOR OUTSTANDING SUMMARY =====

    public List<VendorOutstandingDTO> getVendorOutstandingSummaries() {
        List<Vendor> vendors = vendorRepo.findAll();
        List<VendorOutstandingDTO> summaries = new ArrayList<>();

        for (Vendor vendor : vendors) {
            VendorOutstandingDTO summary = getVendorOutstanding(vendor.getId());
            if (summary.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
                summaries.add(summary);
            }
        }

        // Sort by outstanding amount descending
        summaries.sort((a, b) -> b.getTotalOutstanding().compareTo(a.getTotalOutstanding()));

        return summaries;
    }

    @SuppressWarnings("null")
    public VendorOutstandingDTO getVendorOutstanding(Long vendorId) {
        Vendor vendor = vendorRepo.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        VendorOutstandingDTO summary = new VendorOutstandingDTO();

        // Basic vendor info
        summary.setVendorId(vendor.getId());
        summary.setVendorName(vendor.getName());
        summary.setVendorGstin(vendor.getGstin());
        summary.setContactPerson(vendor.getContactPerson());
        summary.setPhone(vendor.getPhone());

        // Get all invoices for this vendor
        List<PurchaseInvoice> invoices = invoiceRepo.findByVendorId(vendorId);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;
        int unpaidCount = 0;
        int partialCount = 0;
        int overdueCount = 0;
        LocalDate oldestDue = null;

        for (PurchaseInvoice invoice : invoices) {
            totalInvoiced = totalInvoiced
                    .add(invoice.getInvoiceAmount() != null ? invoice.getInvoiceAmount() : BigDecimal.ZERO);
            totalPaid = totalPaid.add(invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO);

            if ("UNPAID".equals(invoice.getPaymentStatus())) {
                unpaidCount++;
            } else if ("PARTIAL".equals(invoice.getPaymentStatus())) {
                partialCount++;
            }

            // Check if overdue
            LocalDate dueDate = invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getInvoiceDate();
            if (dueDate != null && LocalDate.now().isAfter(dueDate) &&
                    !"PAID".equals(invoice.getPaymentStatus())) {
                overdueCount++;
                overdueAmount = overdueAmount
                        .add(invoice.getBalanceDue() != null ? invoice.getBalanceDue() : BigDecimal.ZERO);

                if (oldestDue == null || dueDate.isBefore(oldestDue)) {
                    oldestDue = dueDate;
                }
            }
        }

        summary.setTotalInvoices(invoices.size());
        summary.setTotalInvoiced(totalInvoiced);
        summary.setTotalPaid(totalPaid);
        summary.setTotalOutstanding(totalInvoiced.subtract(totalPaid));
        summary.setUnpaidInvoiceCount(unpaidCount);
        summary.setPartiallyPaidInvoiceCount(partialCount);
        summary.setOverdueInvoiceCount(overdueCount);
        summary.setOverdueAmount(overdueAmount);
        summary.setOldestDueDate(oldestDue);

        return summary;
    }

    // ===== PENDING PAYMENTS =====

    public List<PurchaseInvoice> getPendingInvoices() {
        return invoiceRepo.findByPaymentStatusIn(List.of("UNPAID", "PARTIAL"));
    }

    public List<PurchaseInvoice> getOverdueInvoices() {
        List<PurchaseInvoice> pending = getPendingInvoices();
        LocalDate today = LocalDate.now();

        return pending.stream()
                .filter(invoice -> {
                    LocalDate dueDate = invoice.getDueDate() != null ? invoice.getDueDate() : invoice.getInvoiceDate();
                    return dueDate != null && today.isAfter(dueDate);
                })
                .toList();
    }

    // ===== STATISTICS =====

    public BigDecimal getTotalPaidInPeriod(LocalDate startDate, LocalDate endDate) {
        List<VendorPayment> payments = vendorPaymentRepo.findByPaymentDateBetween(startDate, endDate);
        return payments.stream()
                .map(VendorPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalTdsDeducted(LocalDate startDate, LocalDate endDate) {
        List<VendorPayment> payments = vendorPaymentRepo.findByPaymentDateBetween(startDate, endDate);
        return payments.stream()
                .map(vp -> vp.getTdsDeducted() != null ? vp.getTdsDeducted() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
