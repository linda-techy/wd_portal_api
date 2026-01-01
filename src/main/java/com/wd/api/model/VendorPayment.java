package com.wd.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vendor Payment Entity
 * Tracks payments made to vendors against purchase invoices
 * 
 * Business Context:
 * - Enables accounts payable tracking
 * - TDS deduction as per applicable sections
 * - Multiple payments can be made against one invoice (partial payments)
 */
@Entity
@Table(name = "vendor_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private PurchaseInvoice invoice;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    // Amount Breakdown
    @Column(name = "amount_paid", precision = 15, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "tds_deducted", precision = 15, scale = 2)
    private BigDecimal tdsDeducted;

    @Column(name = "other_deductions", precision = 15, scale = 2)
    private BigDecimal otherDeductions;

    @Column(name = "net_paid", precision = 15, scale = 2, nullable = false)
    private BigDecimal netPaid;

    // Payment Details
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 20)
    private PaymentMode paymentMode;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "cheque_number", length = 50)
    private String chequeNumber;

    @Column(name = "bank_name")
    private String bankName;

    // Tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_id")
    private PortalUser paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private PortalUser approvedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tdsDeducted == null) {
            tdsDeducted = BigDecimal.ZERO;
        }
        if (otherDeductions == null) {
            otherDeductions = BigDecimal.ZERO;
        }
        // Auto-calculate net paid
        calculateNetPaid();
    }

    // Enums
    public enum PaymentMode {
        CASH,
        CHEQUE,
        NEFT,
        RTGS,
        UPI
    }

    // Helper methods
    public void calculateNetPaid() {
        if (amountPaid != null) {
            this.netPaid = amountPaid
                    .subtract(tdsDeducted != null ? tdsDeducted : BigDecimal.ZERO)
                    .subtract(otherDeductions != null ? otherDeductions : BigDecimal.ZERO);
        }
    }

    public boolean isElectronicPayment() {
        return paymentMode == PaymentMode.NEFT ||
                paymentMode == PaymentMode.RTGS ||
                paymentMode == PaymentMode.UPI;
    }

    public boolean requiresBankDetails() {
        return paymentMode == PaymentMode.CHEQUE || isElectronicPayment();
    }

    public BigDecimal getTotalDeductions() {
        BigDecimal total = BigDecimal.ZERO;
        if (tdsDeducted != null) {
            total = total.add(tdsDeducted);
        }
        if (otherDeductions != null) {
            total = total.add(otherDeductions);
        }
        return total;
    }
}
