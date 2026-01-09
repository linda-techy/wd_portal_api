package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

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
public class VendorPayment extends BaseEntity {

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

    // Lifecycle hooks
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
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

    // Constructors
    public VendorPayment() {
    }

    public VendorPayment(Long id, PurchaseInvoice invoice, LocalDate paymentDate, BigDecimal amountPaid,
            BigDecimal tdsDeducted, BigDecimal otherDeductions, BigDecimal netPaid, PaymentMode paymentMode,
            String transactionReference, String chequeNumber, String bankName, PortalUser paidBy, PortalUser approvedBy,
            String notes) {
        this.id = id;
        this.invoice = invoice;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.tdsDeducted = tdsDeducted;
        this.otherDeductions = otherDeductions;
        this.netPaid = netPaid;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.chequeNumber = chequeNumber;
        this.bankName = bankName;
        this.paidBy = paidBy;
        this.approvedBy = approvedBy;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PurchaseInvoice getInvoice() {
        return invoice;
    }

    public void setInvoice(PurchaseInvoice invoice) {
        this.invoice = invoice;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getTdsDeducted() {
        return tdsDeducted;
    }

    public void setTdsDeducted(BigDecimal tdsDeducted) {
        this.tdsDeducted = tdsDeducted;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public BigDecimal getNetPaid() {
        return netPaid;
    }

    public void setNetPaid(BigDecimal netPaid) {
        this.netPaid = netPaid;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public PortalUser getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(PortalUser paidBy) {
        this.paidBy = paidBy;
    }

    public PortalUser getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(PortalUser approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
