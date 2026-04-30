package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Structured "what is included" row attached to a {@link LeadQuotation} (V76).
 *
 * <p>Replaces the legacy practice of stuffing inclusion lists into the
 * quotation's free-text description column. With each inclusion as its own
 * row we can: (a) render them as an icon list on the budgetary PDF rather
 * than wall-of-text, (b) tag by category (Civil / Finishes / MEP / Sanitary)
 * for the detailed PDF section headers, and (c) reuse the same rows on the
 * customer-facing in-app view without re-parsing prose.
 */
@Entity
@Table(name = "quotation_inclusions")
public class QuotationInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private LeadQuotation quotation;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Optional tag for grouping on the detailed PDF. Free-form on purpose —
     * common values are "Civil", "Finishes", "MEP", "Sanitary", "External" —
     * but staff are not boxed in.
     */
    @Column(length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public QuotationInclusion() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LeadQuotation getQuotation() { return quotation; }
    public void setQuotation(LeadQuotation quotation) { this.quotation = quotation; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
