package com.wd.api.model;

import jakarta.persistence.*;
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

    @Column(precision = 15, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "unit_rate", precision = 15, scale = 4)
    private BigDecimal unitRate = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "executed_quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "billed_quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal billedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Override
    @PrePersist
    protected void onCreate() {
        super.onCreate();
        calculateTotalAmount();
        if (status == null) status = "DRAFT";
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
            totalAmount = quantity.multiply(unitRate).setScale(4, RoundingMode.HALF_UP);
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
            return executedQuantity.multiply(unitRate).setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** totalBilledAmount = billedQuantity * unitRate */
    @Transient
    public BigDecimal getTotalBilledAmount() {
        if (billedQuantity != null && unitRate != null) {
            return billedQuantity.multiply(unitRate).setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /** costToComplete = remainingQuantity * unitRate */
    @Transient
    public BigDecimal getCostToComplete() {
        if (unitRate != null) {
            return getRemainingQuantity().multiply(unitRate).setScale(4, RoundingMode.HALF_UP);
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
    public boolean isDraft() { return "DRAFT".equals(status); }
    
    @Transient
    public boolean isApproved() { return "APPROVED".equals(status); }
    
    @Transient
    public boolean isLocked() { return "LOCKED".equals(status); }
    
    @Transient
    public boolean isCompleted() { return "COMPLETED".equals(status); }
    
    @Transient
    public boolean isEditable() { return isDraft() && !isDeleted(); }
    
    @Transient
    public boolean canApprove() { return isDraft(); }
    
    @Transient
    public boolean canLock() { return isApproved(); }
    
    @Transient
    public boolean canExecute() { return isApproved() || isLocked(); }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
