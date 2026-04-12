package com.wd.api.model;

import com.wd.api.model.enums.PaymentStageStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment Stage — one milestone in the agreed payment schedule.
 *
 * Amounts are immutable snapshots computed at BOQ approval time (R-002).
 * They must never be recalculated from live BOQ data after creation.
 *
 * Status flow: UPCOMING → DUE → INVOICED → PAID
 *                                         → OVERDUE
 *                                         → ON_HOLD
 */
@Entity
@Table(name = "payment_stages")
public class PaymentStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_document_id", nullable = false)
    private BoqDocument boqDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    // ---- Immutable snapshot amounts (frozen at BOQ approval) ----

    @Column(name = "boq_value_snapshot", precision = 18, scale = 6, nullable = false)
    private BigDecimal boqValueSnapshot;

    @Column(name = "stage_percentage", precision = 6, scale = 4, nullable = false)
    private BigDecimal stagePercentage;

    @Column(name = "stage_amount_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal stageAmountExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal gstAmount;

    @Column(name = "stage_amount_incl_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal stageAmountInclGst;

    // ---- Status ----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStageStatus status = PaymentStageStatus.UPCOMING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "milestone_description", columnDefinition = "TEXT")
    private String milestoneDescription;

    // ---- Credit note tracking ----

    @Column(name = "applied_credit_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal appliedCreditAmount = BigDecimal.ZERO;

    @Column(name = "net_payable_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal netPayableAmount = BigDecimal.ZERO;

    // ---- Payment tracking ----

    @Column(name = "paid_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_invoice_id")
    private BoqInvoice invoice;

    // ---- Audit ----

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id", updatable = false)
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Version
    @Column(name = "version")
    private Long version = 1L;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PaymentStageStatus.UPCOMING;
        if (appliedCreditAmount == null) appliedCreditAmount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (netPayableAmount == null) netPayableAmount = stageAmountInclGst != null ? stageAmountInclGst : BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Computed helper ----

    /** Recalculates net_payable_amount = stageAmountInclGst - appliedCreditAmount */
    public void recalculateNetPayable() {
        BigDecimal credit = appliedCreditAmount != null ? appliedCreditAmount : BigDecimal.ZERO;
        BigDecimal gross = stageAmountInclGst != null ? stageAmountInclGst : BigDecimal.ZERO;
        this.netPayableAmount = gross.subtract(credit).max(BigDecimal.ZERO);
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BoqDocument getBoqDocument() { return boqDocument; }
    public void setBoqDocument(BoqDocument boqDocument) { this.boqDocument = boqDocument; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public Integer getStageNumber() { return stageNumber; }
    public void setStageNumber(Integer stageNumber) { this.stageNumber = stageNumber; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public BigDecimal getBoqValueSnapshot() { return boqValueSnapshot; }
    public void setBoqValueSnapshot(BigDecimal boqValueSnapshot) { this.boqValueSnapshot = boqValueSnapshot; }

    public BigDecimal getStagePercentage() { return stagePercentage; }
    public void setStagePercentage(BigDecimal stagePercentage) { this.stagePercentage = stagePercentage; }

    public BigDecimal getStageAmountExGst() { return stageAmountExGst; }
    public void setStageAmountExGst(BigDecimal stageAmountExGst) { this.stageAmountExGst = stageAmountExGst; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getStageAmountInclGst() { return stageAmountInclGst; }
    public void setStageAmountInclGst(BigDecimal stageAmountInclGst) { this.stageAmountInclGst = stageAmountInclGst; }

    public PaymentStageStatus getStatus() { return status; }
    public void setStatus(PaymentStageStatus status) { this.status = status; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getMilestoneDescription() { return milestoneDescription; }
    public void setMilestoneDescription(String milestoneDescription) { this.milestoneDescription = milestoneDescription; }

    public BigDecimal getAppliedCreditAmount() { return appliedCreditAmount; }
    public void setAppliedCreditAmount(BigDecimal appliedCreditAmount) { this.appliedCreditAmount = appliedCreditAmount; }

    public BigDecimal getNetPayableAmount() { return netPayableAmount; }
    public void setNetPayableAmount(BigDecimal netPayableAmount) { this.netPayableAmount = netPayableAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public BoqInvoice getInvoice() { return invoice; }
    public void setInvoice(BoqInvoice invoice) { this.invoice = invoice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public void setUpdatedByUserId(Long updatedByUserId) { this.updatedByUserId = updatedByUserId; }
    public Long getVersion() { return version; }
}
