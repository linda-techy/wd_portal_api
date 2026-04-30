package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the milestone-linked payment schedule attached to a
 * {@link LeadQuotation} (V76).
 *
 * <p>Kerala default residential schedule is 8 stages — Booking 10%,
 * Foundation 15%, Plinth 15%, Walls 15%, Slab 15%, Plaster 10%, Flooring
 * 10%, Handover 10%. Staff can deviate per customer; the percentages
 * across all milestones for one quotation should sum to 100, but this is
 * enforced at the service layer (not as a DB CHECK) because intermediate
 * edits would otherwise fail mid-save.
 *
 * <p>{@link #amount} is intentionally nullable: BUDGETARY parents publish
 * the milestone schedule as a structural commitment ("you'll pay in 8
 * stages") without a fixed rupee figure, since the contract value isn't
 * locked yet. DETAILED rows populate with mid-range amounts; CONTRACT_BOQ
 * rows lock to exact rupees.
 */
@Entity
@Table(
    name = "quotation_payment_milestones",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_quotation_milestone_number",
        columnNames = {"quotation_id", "milestone_number"}
    )
)
public class QuotationPaymentMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private LeadQuotation quotation;

    @Column(name = "milestone_number", nullable = false)
    private Integer milestoneNumber;

    /**
     * Customer-facing trigger label, e.g. "On agreement", "Plinth beam
     * complete", "Roof slab cast", "Handover". Free-form so staff can
     * adapt to a specific project's progression without code changes.
     */
    @Column(name = "trigger_event", nullable = false, length = 120)
    private String triggerEvent;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    /**
     * Amount in rupees. {@code null} when the parent quotation is
     * BUDGETARY (total isn't fixed yet). Computed from
     * {@code parent.finalAmount × percentage / 100} for DETAILED and
     * CONTRACT_BOQ at save time by the service layer.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public QuotationPaymentMilestone() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LeadQuotation getQuotation() { return quotation; }
    public void setQuotation(LeadQuotation quotation) { this.quotation = quotation; }

    public Integer getMilestoneNumber() { return milestoneNumber; }
    public void setMilestoneNumber(Integer milestoneNumber) { this.milestoneNumber = milestoneNumber; }

    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
