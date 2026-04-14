package com.wd.api.model;

import com.wd.api.model.enums.BoqDocumentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BOQ Document — the project-level Bill of Quantities container.
 *
 * Method 2 rules enforced here:
 *   R-001  Status APPROVED is terminal — no further edits to scope.
 *   R-002  Financial totals are frozen at submission time.
 *   R-003  All scope changes after APPROVED go through ChangeOrder.
 *
 * Status flow: DRAFT → PENDING_APPROVAL → APPROVED (terminal)
 *                                       → REJECTED  → DRAFT (new revision)
 */
@Entity
@Table(name = "boq_documents")
public class BoqDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "total_value_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalValueExGst = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal gstRate = new BigDecimal("0.18");

    @Column(name = "total_gst_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalGstAmount = BigDecimal.ZERO;

    @Column(name = "total_value_incl_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalValueInclGst = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoqDocumentStatus status = BoqDocumentStatus.DRAFT;

    // Submission
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private PortalUser submittedBy;

    // Internal portal approval
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private PortalUser approvedBy;

    // Customer approval
    @Column(name = "customer_approved_at")
    private LocalDateTime customerApprovedAt;

    @Column(name = "customer_approved_by")
    private Long customerApprovedBy;

    // Rejection
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Customer digital acknowledgement (optional — set by customer app)
    @Column(name = "customer_acknowledged_at")
    private LocalDateTime customerAcknowledgedAt;

    @Column(name = "customer_acknowledged_by")
    private Long customerAcknowledgedBy;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber = 1;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) status = BoqDocumentStatus.DRAFT;
        if (revisionNumber == null) revisionNumber = 1;
        if (totalValueExGst == null) totalValueExGst = BigDecimal.ZERO;
        if (gstRate == null) gstRate = new BigDecimal("0.18");
        if (totalGstAmount == null) totalGstAmount = BigDecimal.ZERO;
        if (totalValueInclGst == null) totalValueInclGst = BigDecimal.ZERO;
    }

    // ---- Status helpers ----

    @Transient
    public boolean isDraft() { return BoqDocumentStatus.DRAFT == status; }

    @Transient
    public boolean isPendingApproval() { return BoqDocumentStatus.PENDING_APPROVAL == status; }

    @Transient
    public boolean isApproved() { return BoqDocumentStatus.APPROVED == status; }

    @Transient
    public boolean isRejected() { return BoqDocumentStatus.REJECTED == status; }

    /** Once approved the BOQ is permanently locked — no edits permitted. */
    @Transient
    public boolean isLocked() { return isApproved(); }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public BigDecimal getTotalValueExGst() { return totalValueExGst; }
    public void setTotalValueExGst(BigDecimal totalValueExGst) { this.totalValueExGst = totalValueExGst; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getTotalGstAmount() { return totalGstAmount; }
    public void setTotalGstAmount(BigDecimal totalGstAmount) { this.totalGstAmount = totalGstAmount; }

    public BigDecimal getTotalValueInclGst() { return totalValueInclGst; }
    public void setTotalValueInclGst(BigDecimal totalValueInclGst) { this.totalValueInclGst = totalValueInclGst; }

    public BoqDocumentStatus getStatus() { return status; }
    public void setStatus(BoqDocumentStatus status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public PortalUser getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(PortalUser submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public PortalUser getApprovedBy() { return approvedBy; }
    public void setApprovedBy(PortalUser approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getCustomerApprovedAt() { return customerApprovedAt; }
    public void setCustomerApprovedAt(LocalDateTime customerApprovedAt) { this.customerApprovedAt = customerApprovedAt; }

    public Long getCustomerApprovedBy() { return customerApprovedBy; }
    public void setCustomerApprovedBy(Long customerApprovedBy) { this.customerApprovedBy = customerApprovedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public Long getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(Long rejectedBy) { this.rejectedBy = rejectedBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

    public LocalDateTime getCustomerAcknowledgedAt() { return customerAcknowledgedAt; }
    public void setCustomerAcknowledgedAt(LocalDateTime customerAcknowledgedAt) { this.customerAcknowledgedAt = customerAcknowledgedAt; }

    public Long getCustomerAcknowledgedBy() { return customerAcknowledgedBy; }
    public void setCustomerAcknowledgedBy(Long customerAcknowledgedBy) { this.customerAcknowledgedBy = customerAcknowledgedBy; }
}
