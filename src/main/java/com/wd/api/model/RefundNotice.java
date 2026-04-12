package com.wd.api.model;

import com.wd.api.model.enums.RefundNoticeStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Refund Notice — raised when a credit note's value exceeds all remaining
 * stage invoices on the project.
 *
 * The excess credit cannot be applied to any future stage invoice, so the
 * company must refund the customer directly.
 *
 * Status flow: PENDING → ACKNOWLEDGED → PROCESSING → COMPLETED
 */
@Entity
@Table(name = "refund_notices")
public class RefundNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @Column(name = "reference_number", nullable = false, length = 50)
    private String referenceNumber;

    @Column(name = "refund_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal refundAmount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundNoticeStatus status = RefundNoticeStatus.PENDING;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (status == null) status = RefundNoticeStatus.PENDING;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public CreditNote getCreditNote() { return creditNote; }
    public void setCreditNote(CreditNote creditNote) { this.creditNote = creditNote; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundNoticeStatus getStatus() { return status; }
    public void setStatus(RefundNoticeStatus status) { this.status = status; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
