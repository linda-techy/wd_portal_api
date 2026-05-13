package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Catalog entry for a design-package tier (Custom / Premium / Bespoke / …).
 *
 * Distinct from {@link DesignPackagePayment}: that table holds the *payment row*
 * created for a specific project; this table is the *catalog template* that
 * marketing / sales admins curate. The customer app reads active rows here
 * to render the tier picker; portal staff manage rows via the admin CRUD.
 */
@Entity
@Table(name = "design_package_templates")
public class DesignPackageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rate_per_sqft", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerSqft;

    @Column(name = "full_payment_discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal fullPaymentDiscountPct = BigDecimal.ZERO;

    @Column(name = "revisions_included", nullable = false)
    private Integer revisionsIncluded = 2;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getRatePerSqft() { return ratePerSqft; }
    public void setRatePerSqft(BigDecimal ratePerSqft) { this.ratePerSqft = ratePerSqft; }
    public BigDecimal getFullPaymentDiscountPct() { return fullPaymentDiscountPct; }
    public void setFullPaymentDiscountPct(BigDecimal v) { this.fullPaymentDiscountPct = v; }
    public Integer getRevisionsIncluded() { return revisionsIncluded; }
    public void setRevisionsIncluded(Integer v) { this.revisionsIncluded = v; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer v) { this.displayOrder = v; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean v) { this.isActive = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long v) { this.createdByUserId = v; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public void setUpdatedByUserId(Long v) { this.updatedByUserId = v; }
}
