package com.wd.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder;

    @Column(name = "po_id", insertable = false, updatable = false)
    private Long poId; // For query purposes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id")
    private GoodsReceivedNote grn;

    @Column(name = "vendor_invoice_number", nullable = false)
    private String vendorInvoiceNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber; // Internal tracking number

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    // New payment tracking fields (added by migration V1_11)
    @Column(name = "invoice_amount", precision = 15, scale = 2)
    private BigDecimal invoiceAmount;

    @Column(name = "gst_amount", precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_due", precision = 15, scale = 2)
    private BigDecimal balanceDue;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // UNPAID, PARTIAL, PAID

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (invoiceAmount == null && amount != null) {
            invoiceAmount = amount;
        }
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        if (gstAmount == null) {
            gstAmount = BigDecimal.ZERO;
        }
        if (paymentStatus == null) {
            paymentStatus = "UNPAID";
        }
    }

    // Helper methods
    public boolean isFullyPaid() {
        return "PAID".equals(paymentStatus);
    }

    public boolean isOverdue() {
        if (dueDate == null || isFullyPaid()) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }
}
