package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Structured "what is NOT included" row attached to a {@link LeadQuotation}
 * (V76).
 *
 * <p>Compound wall, borewell, earth filling, modular kitchen, and furniture
 * are the recurring scope-dispute items in Kerala residential construction.
 * Calling them out explicitly — and giving an honest cost-implication note
 * where possible — converts better than hiding them, contrary to the
 * intuition that admitting exclusions scares the customer off.
 */
@Entity
@Table(name = "quotation_exclusions")
public class QuotationExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private LeadQuotation quotation;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Optional honest range for what the excluded item is likely to cost
     * the customer separately, e.g. "Earth filling: estimate ₹40k–60k
     * extra depending on plot levels". Builds trust by signalling we're
     * not hiding it.
     */
    @Column(name = "cost_implication_note", columnDefinition = "TEXT")
    private String costImplicationNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public QuotationExclusion() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LeadQuotation getQuotation() { return quotation; }
    public void setQuotation(LeadQuotation quotation) { this.quotation = quotation; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCostImplicationNote() { return costImplicationNote; }
    public void setCostImplicationNote(String costImplicationNote) {
        this.costImplicationNote = costImplicationNote;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
