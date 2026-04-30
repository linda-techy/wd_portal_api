package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Structured "we are assuming X about your site / situation" row attached
 * to a {@link LeadQuotation} (V76).
 *
 * <p>Standard examples for Kerala: plot is levelled, motorable road access
 * exists, single-phase electricity is available at site, customer supplies
 * water during construction. Surfaced on every PDF and signed off in the
 * contract — turns implicit assumptions into explicit ones, which removes
 * the most common mid-project surprise.
 */
@Entity
@Table(name = "quotation_assumptions")
public class QuotationAssumption {

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public QuotationAssumption() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LeadQuotation getQuotation() { return quotation; }
    public void setQuotation(LeadQuotation quotation) { this.quotation = quotation; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
