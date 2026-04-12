package com.wd.api.model;

import com.wd.api.model.enums.ChangeOrderStatus;
import com.wd.api.model.enums.ChangeOrderType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Change Order — a formal scope change request raised after the BOQ is approved.
 *
 * Rule R-003 (Method 2): Every scope change after BOQ approval is a Change Order.
 * COs have their own billing lane, completely separate from stage invoices.
 *
 * Status flow: DRAFT → SUBMITTED → CUSTOMER_REVIEW → APPROVED / REJECTED
 *              APPROVED → IN_PROGRESS → COMPLETED → CLOSED
 *
 * On approval of a reduction CO, a CreditNote is auto-generated.
 * On approval of an addition CO, a CO Invoice (advance) is auto-generated.
 */
@Entity
@Table(name = "change_orders")
public class ChangeOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_document_id", nullable = false)
    private BoqDocument boqDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "reference_number", nullable = false, length = 50)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "co_type", nullable = false, length = 40)
    private ChangeOrderType coType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ChangeOrderStatus status = ChangeOrderStatus.DRAFT;

    // Financial summary (aggregated from line items; positive = addition, negative = reduction)
    @Column(name = "net_amount_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal netAmountExGst = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal gstRate = new BigDecimal("0.18");

    @Column(name = "gst_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "net_amount_incl_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal netAmountInclGst = BigDecimal.ZERO;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String justification;

    // Workflow timestamps
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private PortalUser submittedBy;

    @Column(name = "customer_reviewed_at")
    private LocalDateTime customerReviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "review_deadline")
    private LocalDate reviewDeadline;

    @OneToMany(mappedBy = "changeOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChangeOrderLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "changeOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("milestoneNumber ASC")
    private List<ChangeOrderMilestone> milestones = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) status = ChangeOrderStatus.DRAFT;
        if (netAmountExGst == null) netAmountExGst = BigDecimal.ZERO;
        if (gstRate == null) gstRate = new BigDecimal("0.18");
        if (gstAmount == null) gstAmount = BigDecimal.ZERO;
        if (netAmountInclGst == null) netAmountInclGst = BigDecimal.ZERO;
    }

    // ---- Status helpers ----

    @Transient
    public boolean isDraft() { return ChangeOrderStatus.DRAFT == status; }

    @Transient
    public boolean isApproved() { return ChangeOrderStatus.APPROVED == status; }

    @Transient
    public boolean isRejected() { return ChangeOrderStatus.REJECTED == status; }

    @Transient
    public boolean isClosed() { return ChangeOrderStatus.CLOSED == status; }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BoqDocument getBoqDocument() { return boqDocument; }
    public void setBoqDocument(BoqDocument boqDocument) { this.boqDocument = boqDocument; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public ChangeOrderType getCoType() { return coType; }
    public void setCoType(ChangeOrderType coType) { this.coType = coType; }

    public ChangeOrderStatus getStatus() { return status; }
    public void setStatus(ChangeOrderStatus status) { this.status = status; }

    public BigDecimal getNetAmountExGst() { return netAmountExGst; }
    public void setNetAmountExGst(BigDecimal netAmountExGst) { this.netAmountExGst = netAmountExGst; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getNetAmountInclGst() { return netAmountInclGst; }
    public void setNetAmountInclGst(BigDecimal netAmountInclGst) { this.netAmountInclGst = netAmountInclGst; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public PortalUser getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(PortalUser submittedBy) { this.submittedBy = submittedBy; }

    public LocalDateTime getCustomerReviewedAt() { return customerReviewedAt; }
    public void setCustomerReviewedAt(LocalDateTime customerReviewedAt) { this.customerReviewedAt = customerReviewedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public Long getRejectedBy() { return rejectedBy; }
    public void setRejectedBy(Long rejectedBy) { this.rejectedBy = rejectedBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public LocalDate getReviewDeadline() { return reviewDeadline; }
    public void setReviewDeadline(LocalDate reviewDeadline) { this.reviewDeadline = reviewDeadline; }

    public List<ChangeOrderLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<ChangeOrderLineItem> lineItems) { this.lineItems = lineItems; }

    public List<ChangeOrderMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<ChangeOrderMilestone> milestones) { this.milestones = milestones; }
}
