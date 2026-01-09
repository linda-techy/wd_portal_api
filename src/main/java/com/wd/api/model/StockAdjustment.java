package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "adjustment_type", nullable = false, length = 30)
    private String adjustmentType; // WASTAGE, THEFT, DAMAGE, CORRECTION, TRANSFER_OUT, CONSUMPTION

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by_id")
    private PortalUser adjustedBy;

    public StockAdjustment() {
    }

    public StockAdjustment(Long id, CustomerProject project, Material material, String adjustmentType,
            BigDecimal quantity, String reason, PortalUser adjustedBy) {
        this.id = id;
        this.project = project;
        this.material = material;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
    }

    public static StockAdjustmentBuilder builder() {
        return new StockAdjustmentBuilder();
    }

    public static class StockAdjustmentBuilder {
        private Long id;
        private CustomerProject project;
        private Material material;
        private String adjustmentType;
        private BigDecimal quantity;
        private String reason;
        private PortalUser adjustedBy;

        public StockAdjustmentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public StockAdjustmentBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public StockAdjustmentBuilder material(Material material) {
            this.material = material;
            return this;
        }

        public StockAdjustmentBuilder adjustmentType(String adjustmentType) {
            this.adjustmentType = adjustmentType;
            return this;
        }

        public StockAdjustmentBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public StockAdjustmentBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public StockAdjustmentBuilder adjustedBy(PortalUser adjustedBy) {
            this.adjustedBy = adjustedBy;
            return this;
        }

        public StockAdjustment build() {
            return new StockAdjustment(id, project, material, adjustmentType, quantity, reason, adjustedBy);
        }
    }

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

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public PortalUser getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(PortalUser adjustedBy) {
        this.adjustedBy = adjustedBy;
    }
}
