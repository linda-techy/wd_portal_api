package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records how much of a CreditNote was applied to a specific BoqInvoice.
 * Sequential application: oldest DUE/UPCOMING stage invoice first.
 */
@Entity
@Table(name = "credit_note_applications")
public class CreditNoteApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private BoqInvoice invoice;

    @Column(name = "applied_amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal appliedAmount;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by")
    private PortalUser appliedBy;

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CreditNote getCreditNote() { return creditNote; }
    public void setCreditNote(CreditNote creditNote) { this.creditNote = creditNote; }

    public BoqInvoice getInvoice() { return invoice; }
    public void setInvoice(BoqInvoice invoice) { this.invoice = invoice; }

    public BigDecimal getAppliedAmount() { return appliedAmount; }
    public void setAppliedAmount(BigDecimal appliedAmount) { this.appliedAmount = appliedAmount; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public PortalUser getAppliedBy() { return appliedBy; }
    public void setAppliedBy(PortalUser appliedBy) { this.appliedBy = appliedBy; }
}
