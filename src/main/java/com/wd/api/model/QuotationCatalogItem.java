package com.wd.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Quotation Item Catalog — admin-managed master library of reusable
 * quotation line items.
 *
 * Each row represents a stock catalog entry (e.g. "Site clearing and
 * levelling", "Borewell + casing"). Lead-team users pick from this
 * catalog when building quotations; ad-hoc one-off items can later be
 * promoted into the catalog for future reuse.
 *
 * Soft-delete is applied via {@code deleted_at} so historical quotation
 * line items continue to resolve their {@code catalog_item_id} FK even
 * after the catalog row has been retired.
 */
@SQLDelete(sql = "UPDATE quotation_item_catalog SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "quotation_item_catalog")
public class QuotationCatalogItem extends BaseEntity {

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

    @Column(name = "default_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultUnitPrice = BigDecimal.ZERO;

    @Column(name = "times_used", nullable = false)
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (defaultUnitPrice == null) defaultUnitPrice = BigDecimal.ZERO;
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

    public BigDecimal getDefaultUnitPrice() { return defaultUnitPrice; }
    public void setDefaultUnitPrice(BigDecimal defaultUnitPrice) { this.defaultUnitPrice = defaultUnitPrice; }

    public Integer getTimesUsed() { return timesUsed; }
    public void setTimesUsed(Integer timesUsed) { this.timesUsed = timesUsed; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
