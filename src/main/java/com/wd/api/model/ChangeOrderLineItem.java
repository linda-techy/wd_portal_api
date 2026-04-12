package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single work-scope line within a Change Order.
 */
@Entity
@Table(name = "change_order_line_items")
public class ChangeOrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_order_id", nullable = false)
    private ChangeOrder changeOrder;

    /** Optional back-reference to the original BOQ item being changed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 50)
    private String unit;

    @Column(name = "original_quantity", precision = 18, scale = 6)
    private BigDecimal originalQuantity = BigDecimal.ZERO;

    @Column(name = "new_quantity", precision = 18, scale = 6)
    private BigDecimal newQuantity = BigDecimal.ZERO;

    /** delta_quantity = new_quantity - original_quantity (negative = reduction) */
    @Column(name = "delta_quantity", precision = 18, scale = 6)
    private BigDecimal deltaQuantity = BigDecimal.ZERO;

    @Column(name = "original_rate", precision = 18, scale = 6)
    private BigDecimal originalRate = BigDecimal.ZERO;

    @Column(name = "new_rate", precision = 18, scale = 6)
    private BigDecimal newRate = BigDecimal.ZERO;

    @Column(name = "unit_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitRate = BigDecimal.ZERO;

    /** Positive = addition, negative = reduction. */
    @Column(name = "line_amount_ex_gst", precision = 18, scale = 6, nullable = false)
    private BigDecimal lineAmountExGst = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (originalQuantity == null) originalQuantity = BigDecimal.ZERO;
        if (newQuantity == null) newQuantity = BigDecimal.ZERO;
        if (deltaQuantity == null) deltaQuantity = BigDecimal.ZERO;
        if (originalRate == null) originalRate = BigDecimal.ZERO;
        if (newRate == null) newRate = BigDecimal.ZERO;
        if (unitRate == null) unitRate = BigDecimal.ZERO;
        if (lineAmountExGst == null) lineAmountExGst = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChangeOrder getChangeOrder() { return changeOrder; }
    public void setChangeOrder(ChangeOrder changeOrder) { this.changeOrder = changeOrder; }

    public BoqItem getBoqItem() { return boqItem; }
    public void setBoqItem(BoqItem boqItem) { this.boqItem = boqItem; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(BigDecimal originalQuantity) { this.originalQuantity = originalQuantity; }

    public BigDecimal getNewQuantity() { return newQuantity; }
    public void setNewQuantity(BigDecimal newQuantity) { this.newQuantity = newQuantity; }

    public BigDecimal getDeltaQuantity() { return deltaQuantity; }
    public void setDeltaQuantity(BigDecimal deltaQuantity) { this.deltaQuantity = deltaQuantity; }

    public BigDecimal getOriginalRate() { return originalRate; }
    public void setOriginalRate(BigDecimal originalRate) { this.originalRate = originalRate; }

    public BigDecimal getNewRate() { return newRate; }
    public void setNewRate(BigDecimal newRate) { this.newRate = newRate; }

    public BigDecimal getUnitRate() { return unitRate; }
    public void setUnitRate(BigDecimal unitRate) { this.unitRate = unitRate; }

    public BigDecimal getLineAmountExGst() { return lineAmountExGst; }
    public void setLineAmountExGst(BigDecimal lineAmountExGst) { this.lineAmountExGst = lineAmountExGst; }

    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
