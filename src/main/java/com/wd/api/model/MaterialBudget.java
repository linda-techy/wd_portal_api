package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Material Budget - Enterprise Budget Tracking
 * 
 * Tracks approved budgeted rates and quantities for materials per project.
 * Critical for Purchase Order validation - ensures procurement doesn't exceed
 * approved budgets.
 * 
 * Business Rationale:
 * - BoqItem is work-type based (masonry, plastering) not material-based
 * - Need separate tracking: Project + Material + Budgeted Rate/Quantity
 * - POs can be validated against this before approval
 */
@Entity
@Table(name = "material_budgets", uniqueConstraints = @UniqueConstraint(columnNames = { "project_id", "material_id" }))
public class MaterialBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /**
     * Approved budgeted rate per unit for this material in this project
     */
    @Column(name = "budgeted_rate", precision = 15, scale = 2, nullable = false)
    private BigDecimal budgetedRate;

    /**
     * Total approved quantity for this material in this project
     */
    @Column(name = "budgeted_quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal budgetedQuantity;

    /**
     * Total budget allocation (rate × quantity)
     */
    @Column(name = "total_budget", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalBudget;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ==================== Audit Trail ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private PortalUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private PortalUser updatedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private PortalUser deletedByUser;

    @Version
    @Column(name = "version")
    private Long version;

    // ==================== Lifecycle Hooks ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Calculate total budget
        if (budgetedRate != null && budgetedQuantity != null) {
            totalBudget = budgetedRate.multiply(budgetedQuantity);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalculate total budget
        if (budgetedRate != null && budgetedQuantity != null) {
            totalBudget = budgetedRate.multiply(budgetedQuantity);
        }
    }

    // ==================== Constructors ====================

    public MaterialBudget() {
    }

    // ==================== Business Logic ====================

    /**
     * Check if budget is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Calculate variance percentage for a proposed rate
     */
    public BigDecimal calculateVariancePercent(BigDecimal proposedRate) {
        if (budgetedRate == null || budgetedRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal variance = proposedRate.subtract(budgetedRate);
        return variance.divide(budgetedRate, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Check if proposed rate exceeds budget by given threshold percentage
     */
    public boolean exceedsBudgetThreshold(BigDecimal proposedRate, BigDecimal thresholdPercent) {
        BigDecimal variance = calculateVariancePercent(proposedRate);
        return variance.compareTo(thresholdPercent) > 0;
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getBudgetedRate() {
        return budgetedRate;
    }

    public void setBudgetedRate(BigDecimal budgetedRate) {
        this.budgetedRate = budgetedRate;
    }

    public BigDecimal getBudgetedQuantity() {
        return budgetedQuantity;
    }

    public void setBudgetedQuantity(BigDecimal budgetedQuantity) {
        this.budgetedQuantity = budgetedQuantity;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PortalUser getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(PortalUser createdByUser) {
        this.createdByUser = createdByUser;
    }

    public PortalUser getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(PortalUser updatedByUser) {
        this.updatedByUser = updatedByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public PortalUser getDeletedByUser() {
        return deletedByUser;
    }

    public void setDeletedByUser(PortalUser deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
