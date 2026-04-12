package com.wd.api.model;

import com.wd.api.model.enums.CreditNoteStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Credit Note — auto-generated when a reduction Change Order is approved.
 *
 * The credit balance is applied sequentially to the next DUE / UPCOMING stage
 * invoices.  If the credit exceeds all remaining stage invoices a RefundNotice
 * is raised instead.
 *
 * remaining_balance starts equal to total_credit_incl_gst and decreases as
 * CreditNoteApplications are recorded.
 */
@Entity
@Table(name = "credit_notes")
public class CreditNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_order_id", nullable = false)
    private ChangeOrder changeOrder;

    @Column(name = "credit_note_number", nullable = false, length = 50)
    private String creditNoteNumber;

    @Column(name = "credit_amount_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal creditAmountExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal gstRate = new BigDecimal("0.18");

    @Column(name = "gst_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal gstAmount;

    @Column(name = "total_credit_incl_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalCreditInclGst;

    @Column(name = "remaining_balance", precision = 18, scale = 6, nullable = false)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditNoteStatus status = CreditNoteStatus.AVAILABLE;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "fully_applied_at")
    private LocalDateTime fullyAppliedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNoteApplication> applications = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (status == null) status = CreditNoteStatus.AVAILABLE;
        if (remainingBalance == null) remainingBalance = totalCreditInclGst;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }

    public BigDecimal getCreditAmountExGst() { return creditAmountExGst; }
    public void setCreditAmountExGst(BigDecimal creditAmountExGst) { this.creditAmountExGst = creditAmountExGst; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalCreditInclGst() { return totalCreditInclGst; }
    public void setTotalCreditInclGst(BigDecimal totalCreditInclGst) { this.totalCreditInclGst = totalCreditInclGst; }

    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }

    public CreditNoteStatus getStatus() { return status; }
    public void setStatus(CreditNoteStatus status) { this.status = status; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getFullyAppliedAt() { return fullyAppliedAt; }
    public void setFullyAppliedAt(LocalDateTime fullyAppliedAt) { this.fullyAppliedAt = fullyAppliedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<CreditNoteApplication> getApplications() { return applications; }
    public void setApplications(List<CreditNoteApplication> applications) { this.applications = applications; }
}
