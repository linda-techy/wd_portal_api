package com.wd.api.model;

import com.wd.api.model.enums.ProjectInvoiceStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BOQ Invoice — covers both stage invoices and CO invoices (Method 2).
 *
 * invoice_type = STAGE_INVOICE: tied to a PaymentStage; contains only stage
 *   amount + GST + any applied credit note deductions. Never CO amounts.
 *
 * invoice_type = CO_INVOICE: tied to a ChangeOrder; covers ADVANCE / MIDPOINT /
 *   BALANCE billing events. Completely separate billing lane.
 *
 * Status flow: DRAFT → SENT → VIEWED → PAID
 *                                     → DISPUTED → PAID
 *                                     → OVERDUE  → PAID
 */
@Entity
@Table(name = "boq_invoices")
public class BoqInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "invoice_type", nullable = false, length = 20)
    private String invoiceType;  // STAGE_INVOICE | CO_INVOICE

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_stage_id")
    private PaymentStage paymentStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_order_id")
    private ChangeOrder changeOrder;

    /** ADVANCE / MIDPOINT / BALANCE — only for CO_INVOICE. */
    @Column(name = "co_billing_event", length = 20)
    private String coBillingEvent;

    @Column(name = "subtotal_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal subtotalExGst = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal gstRate = new BigDecimal("0.18");

    @Column(name = "gst_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_incl_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalInclGst = BigDecimal.ZERO;

    @Column(name = "total_credit_applied", precision = 18, scale = 6, nullable = false)
    private BigDecimal totalCreditApplied = BigDecimal.ZERO;

    @Column(name = "net_amount_due", precision = 18, scale = 6, nullable = false)
    private BigDecimal netAmountDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectInvoiceStatus status = ProjectInvoiceStatus.DRAFT;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<BoqInvoiceLineItem> lineItems = new ArrayList<>();

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) status = ProjectInvoiceStatus.DRAFT;
        if (subtotalExGst == null) subtotalExGst = BigDecimal.ZERO;
        if (gstRate == null) gstRate = new BigDecimal("0.18");
        if (gstAmount == null) gstAmount = BigDecimal.ZERO;
        if (totalInclGst == null) totalInclGst = BigDecimal.ZERO;
        if (totalCreditApplied == null) totalCreditApplied = BigDecimal.ZERO;
        if (netAmountDue == null) netAmountDue = BigDecimal.ZERO;
    }

    // ---- Status helpers ----

    @Transient
    public boolean isDraft() { return ProjectInvoiceStatus.DRAFT == status; }

    @Transient
    public boolean isSent() { return ProjectInvoiceStatus.SENT == status; }

    @Transient
    public boolean isPaid() { return ProjectInvoiceStatus.PAID == status; }

    @Transient
    public boolean isVoid() { return ProjectInvoiceStatus.VOID == status; }

    @Transient
    public boolean isStageInvoice() { return "STAGE_INVOICE".equals(invoiceType); }

    @Transient
    public boolean isCoInvoice() { return "CO_INVOICE".equals(invoiceType); }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public PaymentStage getPaymentStage() { return paymentStage; }
    public void setPaymentStage(PaymentStage paymentStage) { this.paymentStage = paymentStage; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public String getCoBillingEvent() { return coBillingEvent; }
    public void setCoBillingEvent(String coBillingEvent) { this.coBillingEvent = coBillingEvent; }

    public BigDecimal getSubtotalExGst() { return subtotalExGst; }
    public void setSubtotalExGst(BigDecimal subtotalExGst) { this.subtotalExGst = subtotalExGst; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalInclGst() { return totalInclGst; }
    public void setTotalInclGst(BigDecimal totalInclGst) { this.totalInclGst = totalInclGst; }

    public BigDecimal getTotalCreditApplied() { return totalCreditApplied; }
    public void setTotalCreditApplied(BigDecimal totalCreditApplied) { this.totalCreditApplied = totalCreditApplied; }

    public BigDecimal getNetAmountDue() { return netAmountDue; }
    public void setNetAmountDue(BigDecimal netAmountDue) { this.netAmountDue = netAmountDue; }

    public ProjectInvoiceStatus getStatus() { return status; }
    public void setStatus(ProjectInvoiceStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getDisputedAt() { return disputedAt; }
    public void setDisputedAt(LocalDateTime disputedAt) { this.disputedAt = disputedAt; }

    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String disputeReason) { this.disputeReason = disputeReason; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<BoqInvoiceLineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<BoqInvoiceLineItem> lineItems) { this.lineItems = lineItems; }
}
