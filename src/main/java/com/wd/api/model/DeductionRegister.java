package com.wd.api.model;

import com.wd.api.model.enums.DeductionDecision;
import com.wd.api.model.enums.EscalationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deduction Register — tracks omission/scope-reduction deductions per project.
 *
 * Lifecycle:
 *   decision: PENDING → ACCEPTABLE / PARTIALLY_ACCEPTABLE / REJECTED
 *   escalation: NONE → ESCALATED → RESOLVED
 *
 * A deduction may optionally reference the OMISSION/SCOPE_REDUCTION ChangeOrder that raised it.
 * Once settled in the final account, settled_in_final_account = true.
 */
@Entity
@Table(name = "deduction_register")
public class DeductionRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    /** Optional link to the OMISSION CO that generated this deduction. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_id")
    private ChangeOrder changeOrder;

    @Column(name = "item_description", columnDefinition = "TEXT", nullable = false)
    private String itemDescription;

    @Column(name = "requested_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal requestedAmount;

    /** Final settled amount — null until decision is made. */
    @Column(name = "accepted_amount", precision = 18, scale = 6)
    private BigDecimal acceptedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private DeductionDecision decision = DeductionDecision.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_status", nullable = false, length = 20)
    private EscalationStatus escalationStatus = EscalationStatus.NONE;

    /** Name/title of the person the deduction was escalated to. */
    @Column(name = "escalated_to", length = 100)
    private String escalatedTo;

    @Column(name = "settled_in_final_account", nullable = false)
    private boolean settledInFinalAccount = false;

    /** Name of the person who approved/decided the deduction. */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (decision == null) decision = DeductionDecision.PENDING;
        if (escalationStatus == null) escalationStatus = EscalationStatus.NONE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Business helpers ----

    @Transient
    public boolean isPending() { return DeductionDecision.PENDING == decision; }

    @Transient
    public boolean isEscalated() { return EscalationStatus.ESCALATED == escalationStatus; }

    // ---- Getters and Setters ----

    public Long getId() { return id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = acceptedAmount; }

    public DeductionDecision getDecision() { return decision; }
    public void setDecision(DeductionDecision decision) { this.decision = decision; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public EscalationStatus getEscalationStatus() { return escalationStatus; }
    public void setEscalationStatus(EscalationStatus escalationStatus) { this.escalationStatus = escalationStatus; }

    public String getEscalatedTo() { return escalatedTo; }
    public void setEscalatedTo(String escalatedTo) { this.escalatedTo = escalatedTo; }

    public boolean isSettledInFinalAccount() { return settledInFinalAccount; }
    public void setSettledInFinalAccount(boolean settledInFinalAccount) { this.settledInFinalAccount = settledInFinalAccount; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDate getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDate decisionDate) { this.decisionDate = decisionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
