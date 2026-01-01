package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subcontract_payments")
public class SubcontractPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private SubcontractWorkOrder workOrder;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    // Amount Breakdown
    @Column(name = "gross_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "tds_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal tdsPercentage; // 1% or 2%

    @Column(name = "tds_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal tdsAmount;

    @Column(name = "other_deductions", precision = 15, scale = 2)
    private BigDecimal otherDeductions;

    @Column(name = "net_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal netAmount;

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

    // Milestone (for lump-sum contracts)
    @Column(name = "milestone_description")
    private String milestoneDescription;

    @Column(name = "milestone_percentage", precision = 5, scale = 2)
    private BigDecimal milestonePercentage;

    @Column(name = "is_advance_payment")
    private Boolean isAdvancePayment;

    // Link to measurements (for unit-rate contracts)
    @Column(name = "measurement_ids", columnDefinition = "BIGINT[]")
    private Long[] measurementIds; // Array of measurement IDs covered by this payment

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
        if (isAdvancePayment == null) {
            isAdvancePayment = false;
        }
        if (otherDeductions == null) {
            otherDeductions = BigDecimal.ZERO;
        }
        // Auto-calculate TDS and net amount
        calculateAmounts();
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
    public void calculateAmounts() {
        if (grossAmount != null && tdsPercentage != null) {
            // Calculate TDS
            this.tdsAmount = grossAmount.multiply(tdsPercentage)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            // Calculate net amount
            this.netAmount = grossAmount.subtract(tdsAmount);
            if (otherDeductions != null) {
                this.netAmount = netAmount.subtract(otherDeductions);
            }
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

    public static BigDecimal getStandardTdsPercentage(boolean isCompany) {
        // Section 194C: 1% for individuals, 2% for companies
        return isCompany ? new BigDecimal("2.00") : new BigDecimal("1.00");
    }

    public SubcontractPayment() {
    }

    public SubcontractPayment(Long id, SubcontractWorkOrder workOrder, LocalDate paymentDate, BigDecimal grossAmount,
            BigDecimal tdsPercentage, BigDecimal tdsAmount, BigDecimal otherDeductions, BigDecimal netAmount,
            PaymentMode paymentMode, String transactionReference, String chequeNumber, String bankName,
            String milestoneDescription, BigDecimal milestonePercentage, Boolean isAdvancePayment,
            Long[] measurementIds, PortalUser paidBy, PortalUser approvedBy, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.workOrder = workOrder;
        this.paymentDate = paymentDate;
        this.grossAmount = grossAmount;
        this.tdsPercentage = tdsPercentage;
        this.tdsAmount = tdsAmount;
        this.otherDeductions = otherDeductions;
        this.netAmount = netAmount;
        this.paymentMode = paymentMode;
        this.transactionReference = transactionReference;
        this.chequeNumber = chequeNumber;
        this.bankName = bankName;
        this.milestoneDescription = milestoneDescription;
        this.milestonePercentage = milestonePercentage;
        this.isAdvancePayment = isAdvancePayment;
        this.measurementIds = measurementIds;
        this.paidBy = paidBy;
        this.approvedBy = approvedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static SubcontractPaymentBuilder builder() {
        return new SubcontractPaymentBuilder();
    }

    public static class SubcontractPaymentBuilder {
        private Long id;
        private SubcontractWorkOrder workOrder;
        private LocalDate paymentDate;
        private BigDecimal grossAmount;
        private BigDecimal tdsPercentage;
        private BigDecimal tdsAmount;
        private BigDecimal otherDeductions;
        private BigDecimal netAmount;
        private PaymentMode paymentMode;
        private String transactionReference;
        private String chequeNumber;
        private String bankName;
        private String milestoneDescription;
        private BigDecimal milestonePercentage;
        private Boolean isAdvancePayment;
        private Long[] measurementIds;
        private PortalUser paidBy;
        private PortalUser approvedBy;
        private String notes;
        private LocalDateTime createdAt;

        public SubcontractPaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SubcontractPaymentBuilder workOrder(SubcontractWorkOrder workOrder) {
            this.workOrder = workOrder;
            return this;
        }

        public SubcontractPaymentBuilder paymentDate(LocalDate paymentDate) {
            this.paymentDate = paymentDate;
            return this;
        }

        public SubcontractPaymentBuilder grossAmount(BigDecimal grossAmount) {
            this.grossAmount = grossAmount;
            return this;
        }

        public SubcontractPaymentBuilder tdsPercentage(BigDecimal tdsPercentage) {
            this.tdsPercentage = tdsPercentage;
            return this;
        }

        public SubcontractPaymentBuilder tdsAmount(BigDecimal tdsAmount) {
            this.tdsAmount = tdsAmount;
            return this;
        }

        public SubcontractPaymentBuilder otherDeductions(BigDecimal otherDeductions) {
            this.otherDeductions = otherDeductions;
            return this;
        }

        public SubcontractPaymentBuilder netAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public SubcontractPaymentBuilder paymentMode(PaymentMode paymentMode) {
            this.paymentMode = paymentMode;
            return this;
        }

        public SubcontractPaymentBuilder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public SubcontractPaymentBuilder chequeNumber(String chequeNumber) {
            this.chequeNumber = chequeNumber;
            return this;
        }

        public SubcontractPaymentBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public SubcontractPaymentBuilder milestoneDescription(String milestoneDescription) {
            this.milestoneDescription = milestoneDescription;
            return this;
        }

        public SubcontractPaymentBuilder milestonePercentage(BigDecimal milestonePercentage) {
            this.milestonePercentage = milestonePercentage;
            return this;
        }

        public SubcontractPaymentBuilder isAdvancePayment(Boolean isAdvancePayment) {
            this.isAdvancePayment = isAdvancePayment;
            return this;
        }

        public SubcontractPaymentBuilder measurementIds(Long[] measurementIds) {
            this.measurementIds = measurementIds;
            return this;
        }

        public SubcontractPaymentBuilder paidBy(PortalUser paidBy) {
            this.paidBy = paidBy;
            return this;
        }

        public SubcontractPaymentBuilder approvedBy(PortalUser approvedBy) {
            this.approvedBy = approvedBy;
            return this;
        }

        public SubcontractPaymentBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public SubcontractPaymentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SubcontractPayment build() {
            return new SubcontractPayment(id, workOrder, paymentDate, grossAmount, tdsPercentage, tdsAmount,
                    otherDeductions, netAmount, paymentMode, transactionReference, chequeNumber, bankName,
                    milestoneDescription, milestonePercentage, isAdvancePayment, measurementIds, paidBy, approvedBy,
                    notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public SubcontractWorkOrder getWorkOrder() {
        return workOrder;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getTdsPercentage() {
        return tdsPercentage;
    }

    public BigDecimal getTdsAmount() {
        return tdsAmount;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public String getMilestoneDescription() {
        return milestoneDescription;
    }

    public BigDecimal getMilestonePercentage() {
        return milestonePercentage;
    }

    public Boolean getIsAdvancePayment() {
        return isAdvancePayment;
    }

    public Long[] getMeasurementIds() {
        return measurementIds;
    }

    public PortalUser getPaidBy() {
        return paidBy;
    }

    public PortalUser getApprovedBy() {
        return approvedBy;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setWorkOrder(SubcontractWorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public void setTdsPercentage(BigDecimal tdsPercentage) {
        this.tdsPercentage = tdsPercentage;
    }

    public void setTdsAmount(BigDecimal tdsAmount) {
        this.tdsAmount = tdsAmount;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setMilestoneDescription(String milestoneDescription) {
        this.milestoneDescription = milestoneDescription;
    }

    public void setMilestonePercentage(BigDecimal milestonePercentage) {
        this.milestonePercentage = milestonePercentage;
    }

    public void setIsAdvancePayment(Boolean isAdvancePayment) {
        this.isAdvancePayment = isAdvancePayment;
    }

    public void setMeasurementIds(Long[] measurementIds) {
        this.measurementIds = measurementIds;
    }

    public void setPaidBy(PortalUser paidBy) {
        this.paidBy = paidBy;
    }

    public void setApprovedBy(PortalUser approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
