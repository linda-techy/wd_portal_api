package com.wd.api.model;

import com.wd.api.model.enums.COPaymentStatus;
import com.wd.api.model.enums.VOCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment schedule for an approved Variation Order — three tranches:
 *   Advance → Progress (triggered by a stage) → Completion (triggered by final account).
 *
 * Default splits by category:
 *   MATERIAL_HEAVY: 40 / 40 / 20
 *   LABOUR_HEAVY:   20 / 60 / 20
 *   MIXED:          30 / 50 / 20
 *   CUSTOM:         user-defined (must still sum to 100)
 *
 * One row per approved CO (UNIQUE on co_id).
 */
@Entity
@Table(name = "co_payment_schedule")
public class ChangeOrderPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_id", nullable = false, unique = true)
    private ChangeOrder changeOrder;

    // ---- Advance tranche ----

    @Column(name = "advance_pct", nullable = false)
    private Integer advancePct = 30;

    @Column(name = "advance_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "advance_status", nullable = false, length = 20)
    private COPaymentStatus advanceStatus = COPaymentStatus.PENDING;

    @Column(name = "advance_due_date")
    private LocalDate advanceDueDate;

    @Column(name = "advance_paid_date")
    private LocalDate advancePaidDate;

    @Column(name = "advance_invoice_number", length = 50)
    private String advanceInvoiceNumber;

    // ---- Progress tranche ----

    @Column(name = "progress_pct", nullable = false)
    private Integer progressPct = 50;

    @Column(name = "progress_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal progressAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 20)
    private COPaymentStatus progressStatus = COPaymentStatus.PENDING;

    /** Stage whose completion triggers the progress invoice. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_trigger_stage_id")
    private PaymentStage progressTriggerStage;

    @Column(name = "progress_paid_date")
    private LocalDate progressPaidDate;

    // ---- Completion tranche ----

    @Column(name = "completion_pct", nullable = false)
    private Integer completionPct = 20;

    @Column(name = "completion_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal completionAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 20)
    private COPaymentStatus completionStatus = COPaymentStatus.PENDING;

    /** Trigger condition description (default: "FINAL_ACCOUNT_APPROVED"). */
    @Column(name = "completion_trigger", nullable = false, length = 50)
    private String completionTrigger = "FINAL_ACCOUNT_APPROVED";

    @Column(name = "completion_paid_date")
    private LocalDate completionPaidDate;

    // ---- Audit ----

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        applyDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Initialises percentage splits from the VO's category and recomputes tranche amounts
     * from the VO's approved_cost.
     */
    public void applyCategory(VOCategory category, BigDecimal approvedCost) {
        if (category != null && category != VOCategory.CUSTOM) {
            this.advancePct    = category.defaultAdvancePct();
            this.progressPct   = category.defaultProgressPct();
            this.completionPct = category.defaultCompletionPct();
        }
        recomputeAmounts(approvedCost);
    }

    /** Recomputes tranche amounts from the supplied total (e.g. approved_cost). */
    public void recomputeAmounts(BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal hundred = new BigDecimal(100);
        this.advanceAmount    = total.multiply(BigDecimal.valueOf(advancePct)).divide(hundred, 6, java.math.RoundingMode.HALF_UP);
        this.progressAmount   = total.multiply(BigDecimal.valueOf(progressPct)).divide(hundred, 6, java.math.RoundingMode.HALF_UP);
        this.completionAmount = total.subtract(advanceAmount).subtract(progressAmount);
    }

    private void applyDefaults() {
        if (advanceStatus == null)    advanceStatus    = COPaymentStatus.PENDING;
        if (progressStatus == null)   progressStatus   = COPaymentStatus.PENDING;
        if (completionStatus == null) completionStatus = COPaymentStatus.PENDING;
        if (completionTrigger == null) completionTrigger = "FINAL_ACCOUNT_APPROVED";
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public Integer getAdvancePct() { return advancePct; }
    public void setAdvancePct(Integer advancePct) { this.advancePct = advancePct; }

    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public void setAdvanceAmount(BigDecimal advanceAmount) { this.advanceAmount = advanceAmount; }

    public COPaymentStatus getAdvanceStatus() { return advanceStatus; }
    public void setAdvanceStatus(COPaymentStatus advanceStatus) { this.advanceStatus = advanceStatus; }

    public LocalDate getAdvanceDueDate() { return advanceDueDate; }
    public void setAdvanceDueDate(LocalDate advanceDueDate) { this.advanceDueDate = advanceDueDate; }

    public LocalDate getAdvancePaidDate() { return advancePaidDate; }
    public void setAdvancePaidDate(LocalDate advancePaidDate) { this.advancePaidDate = advancePaidDate; }

    public String getAdvanceInvoiceNumber() { return advanceInvoiceNumber; }
    public void setAdvanceInvoiceNumber(String advanceInvoiceNumber) { this.advanceInvoiceNumber = advanceInvoiceNumber; }

    public Integer getProgressPct() { return progressPct; }
    public void setProgressPct(Integer progressPct) { this.progressPct = progressPct; }

    public BigDecimal getProgressAmount() { return progressAmount; }
    public void setProgressAmount(BigDecimal progressAmount) { this.progressAmount = progressAmount; }

    public COPaymentStatus getProgressStatus() { return progressStatus; }
    public void setProgressStatus(COPaymentStatus progressStatus) { this.progressStatus = progressStatus; }

    public PaymentStage getProgressTriggerStage() { return progressTriggerStage; }
    public void setProgressTriggerStage(PaymentStage progressTriggerStage) { this.progressTriggerStage = progressTriggerStage; }

    public LocalDate getProgressPaidDate() { return progressPaidDate; }
    public void setProgressPaidDate(LocalDate progressPaidDate) { this.progressPaidDate = progressPaidDate; }

    public Integer getCompletionPct() { return completionPct; }
    public void setCompletionPct(Integer completionPct) { this.completionPct = completionPct; }

    public BigDecimal getCompletionAmount() { return completionAmount; }
    public void setCompletionAmount(BigDecimal completionAmount) { this.completionAmount = completionAmount; }

    public COPaymentStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(COPaymentStatus completionStatus) { this.completionStatus = completionStatus; }

    public String getCompletionTrigger() { return completionTrigger; }
    public void setCompletionTrigger(String completionTrigger) { this.completionTrigger = completionTrigger; }

    public LocalDate getCompletionPaidDate() { return completionPaidDate; }
    public void setCompletionPaidDate(LocalDate completionPaidDate) { this.completionPaidDate = completionPaidDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
