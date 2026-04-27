package com.wd.api.model;

import com.wd.api.model.enums.DpcCustomizationSource;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Itemized variance row on the DPC customizations page.
 *
 * Two sources:
 *  - AUTO_FROM_BOQ_ADDON  generated from an ADDON BoQ item; refreshed on demand.
 *  - MANUAL               added by an editor; preserved across regeneration.
 *
 * The optional {@code boqItemId} backlink lets the regenerator find and update
 * AUTO rows in place rather than re-creating them.
 */
@SQLDelete(sql = "UPDATE dpc_customization_line SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "dpc_customization_line")
public class DpcCustomizationLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dpc_document_id", nullable = false)
    private DpcDocument dpcDocument;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DpcCustomizationSource source = DpcCustomizationSource.MANUAL;

    /** Backlink to the BoQ item that produced an AUTO row (nullable for MANUAL). */
    @Column(name = "boq_item_id")
    private Long boqItemId;

    /**
     * Optional link to the master catalog row this MANUAL line was sourced from.
     * NULL for AUTO_FROM_BOQ_ADDON rows and for MANUAL rows entered ad-hoc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customization_catalog_id")
    private DpcCustomizationCatalogItem catalogItem;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (displayOrder == null) displayOrder = 0;
        if (amount == null) amount = BigDecimal.ZERO;
        if (source == null) source = DpcCustomizationSource.MANUAL;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DpcDocument getDpcDocument() { return dpcDocument; }
    public void setDpcDocument(DpcDocument dpcDocument) { this.dpcDocument = dpcDocument; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public DpcCustomizationSource getSource() { return source; }
    public void setSource(DpcCustomizationSource source) { this.source = source; }

    public Long getBoqItemId() { return boqItemId; }
    public void setBoqItemId(Long boqItemId) { this.boqItemId = boqItemId; }

    public DpcCustomizationCatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(DpcCustomizationCatalogItem catalogItem) { this.catalogItem = catalogItem; }

    /** True when this MANUAL line was sourced from the catalog. */
    @Transient
    public boolean isFromCatalog() {
        return catalogItem != null;
    }
}
