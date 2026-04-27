package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * DPC Customization Catalog — admin-managed master library of reusable
 * DPC customization line items.
 *
 * Each row represents a stock catalog entry (e.g. "Foundation upgrade —
 * RR to column footing", "Texture paint on elevation walls"). DPC editors
 * pick from this catalog while building a DPC; ad-hoc one-off lines can
 * later be promoted into the catalog for future reuse.
 *
 * Soft-delete is applied via {@code deleted_at} so historical
 * {@link DpcCustomizationLine} rows continue to resolve their
 * {@code customization_catalog_id} FK even after the catalog row has
 * been retired.
 *
 * Differs from {@link QuotationCatalogItem} only in that customization
 * lines are LUMP-SUM amounts (no quantity), so this entity stores
 * {@code defaultAmount} rather than {@code defaultUnitPrice}.
 */
@SQLDelete(sql = "UPDATE dpc_customization_catalog SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_customization_catalog")
public class DpcCustomizationCatalogItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 80)
    private String category;

    @Column(length = 40)
    private String unit;

    @Column(name = "default_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal defaultAmount = BigDecimal.ZERO;

    @Column(name = "times_used", nullable = false)
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (defaultAmount == null) defaultAmount = BigDecimal.ZERO;
        if (timesUsed == null) timesUsed = 0;
        if (isActive == null) isActive = true;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }

    public Integer getTimesUsed() { return timesUsed; }
    public void setTimesUsed(Integer timesUsed) { this.timesUsed = timesUsed; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
