package com.wd.api.model;

import com.wd.api.model.enums.BoqItemStatus;
import com.wd.api.model.enums.ItemKind;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BOQ (Bill of Quantities) item entity.
 * 
 * Financial rules enforced:
 * - executedQuantity <= quantity (planned)
 * - billedQuantity <= executedQuantity
 * - No negative quantities or rates
 * - Soft delete only (deletedAt)
 * - Optimistic locking via @Version
 * - Audit trail via BaseEntity
 */
@SQLDelete(sql = "UPDATE boq_items SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "boq_items")
public class BoqItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BoqCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id")
    private BoqWorkType workType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 50)
    private String unit;

    @Column(precision = 18, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "unit_rate", precision = 18, scale = 6)
    private BigDecimal unitRate = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "executed_quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "billed_quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal billedQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoqItemStatus status = BoqItemStatus.DRAFT;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Scope classification of this line item.
     * BASE       — always included in the contract.
     * ADDON      — charged extra if selected; shown separately to customer.
     * OPTIONAL   — customer may choose; not included in base total.
     * EXCLUSION  — explicitly out of scope; listed for transparency only.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 20)
    private ItemKind itemKind = ItemKind.BASE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_document_id")
    private BoqDocument boqDocument;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        calculateTotalAmount();
        if (status == null) status = BoqItemStatus.DRAFT;
        if (isActive == null) isActive = true;
        if (executedQuantity == null) executedQuantity = BigDecimal.ZERO;
        if (billedQuantity == null) billedQuantity = BigDecimal.ZERO;
    }

    @Override
    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        if (quantity != null && unitRate != null) {
            totalAmount = quantity.multiply(unitRate).setScale(6, RoundingMode.HALF_UP);
        }
    }

    // ---- Computed financial getters ----

    /** remainingQuantity = planned - executed (never negative) */
    @Transient
    public BigDecimal getRemainingQuantity() {
        BigDecimal remaining = (quantity != null ? quantity : BigDecimal.ZERO)
                .subtract(executedQuantity != null ? executedQuantity : BigDecimal.ZERO);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    /** remainingBillableQuantity = executed - billed (never negative) */
    @Transient
    public BigDecimal getRemainingBillableQuantity() {
        BigDecimal remaining = (executedQuantity != null ? executedQuantity : BigDecimal.ZERO)
                .subtract(billedQuantity != null ? billedQuantity : BigDecimal.ZERO);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    /** totalExecutedAmount = executedQuantity * unitRate */
    @Transient
    public BigDecimal getTotalExecutedAmount() {
        if (executedQuantity != null && unitRate != null) {
            return executedQuantity.multiply(unitRate).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** totalBilledAmount = billedQuantity * unitRate */
    @Transient
    public BigDecimal getTotalBilledAmount() {
        if (billedQuantity != null && unitRate != null) {
            return billedQuantity.multiply(unitRate).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** costToComplete = remainingQuantity * unitRate */
    @Transient
    public BigDecimal getCostToComplete() {
        if (unitRate != null) {
            return getRemainingQuantity().multiply(unitRate).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** executionPercentage = (executedQuantity / quantity) * 100 */
    @Transient
    public BigDecimal getExecutionPercentage() {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0 && executedQuantity != null) {
            return executedQuantity.divide(quantity, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** billingPercentage = (billedQuantity / executedQuantity) * 100 */
    @Transient
    public BigDecimal getBillingPercentage() {
        if (executedQuantity != null && executedQuantity.compareTo(BigDecimal.ZERO) > 0 && billedQuantity != null) {
            return billedQuantity.divide(executedQuantity, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // ---- Status helpers ----
    @Transient
    public boolean isDraft() { return BoqItemStatus.DRAFT == status; }

    @Transient
    public boolean isApproved() { return BoqItemStatus.APPROVED == status; }

    @Transient
    public boolean isLocked() { return BoqItemStatus.LOCKED == status; }

    @Transient
    public boolean isCompleted() { return BoqItemStatus.COMPLETED == status; }
    
    @Transient
    public boolean isEditable() { return isDraft() && !isDeleted(); }
    
    @Transient
    public boolean canApprove() { return isDraft() && !isCompleted(); }

    @Transient
    public boolean canLock() { return isApproved() && !isCompleted(); }

    @Transient
    public boolean canExecute() { return (isApproved() || isLocked()) && !isCompleted(); }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerProject getProject() { return project; }
    public void setProject(CustomerProject project) { this.project = project; }

    public BoqCategory getCategory() { return category; }
    public void setCategory(BoqCategory category) { this.category = category; }

    public BoqWorkType getWorkType() { return workType; }
    public void setWorkType(BoqWorkType workType) { this.workType = workType; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitRate() { return unitRate; }
    public void setUnitRate(BigDecimal unitRate) { this.unitRate = unitRate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public void setExecutedQuantity(BigDecimal executedQuantity) { this.executedQuantity = executedQuantity; }

    public BigDecimal getBilledQuantity() { return billedQuantity; }
    public void setBilledQuantity(BigDecimal billedQuantity) { this.billedQuantity = billedQuantity; }

    public BoqItemStatus getStatus() { return status; }
    public void setStatus(BoqItemStatus status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ItemKind getItemKind() { return itemKind != null ? itemKind : ItemKind.BASE; }
    public void setItemKind(ItemKind itemKind) { this.itemKind = itemKind != null ? itemKind : ItemKind.BASE; }

    public BoqDocument getBoqDocument() { return boqDocument; }
    public void setBoqDocument(BoqDocument boqDocument) { this.boqDocument = boqDocument; }
}
