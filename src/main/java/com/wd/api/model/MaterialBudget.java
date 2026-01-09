package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

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
public class MaterialBudget extends BaseEntity {

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

    // ==================== Lifecycle Hooks ====================

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        // Calculate total budget
        if (budgetedRate != null && budgetedQuantity != null) {
            totalBudget = budgetedRate.multiply(budgetedQuantity);
        }
    }

    @PreUpdate
    protected void onUpdate() {
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
}
