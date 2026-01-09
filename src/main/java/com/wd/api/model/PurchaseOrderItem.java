package com.wd.api.model;

import com.wd.api.model.enums.MaterialUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MaterialUnit unit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage = new BigDecimal("18.00");

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    public PurchaseOrderItem() {
    }

    public PurchaseOrderItem(Long id, PurchaseOrder purchaseOrder, Material material, String description,
            BigDecimal quantity, MaterialUnit unit, BigDecimal rate, BigDecimal gstPercentage, BigDecimal amount) {
        this.id = id;
        this.purchaseOrder = purchaseOrder;
        this.material = material;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.rate = rate;
        this.gstPercentage = gstPercentage;
        this.amount = amount;
    }

    public static PurchaseOrderItemBuilder builder() {
        return new PurchaseOrderItemBuilder();
    }

    public static class PurchaseOrderItemBuilder {
        private Long id;
        private PurchaseOrder purchaseOrder;
        private Material material;
        private String description;
        private BigDecimal quantity;
        private MaterialUnit unit;
        private BigDecimal rate;
        private BigDecimal gstPercentage = new BigDecimal("18.00");
        private BigDecimal amount;

        public PurchaseOrderItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PurchaseOrderItemBuilder purchaseOrder(PurchaseOrder purchaseOrder) {
            this.purchaseOrder = purchaseOrder;
            return this;
        }

        public PurchaseOrderItemBuilder material(Material material) {
            this.material = material;
            return this;
        }

        public PurchaseOrderItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PurchaseOrderItemBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public PurchaseOrderItemBuilder unit(MaterialUnit unit) {
            this.unit = unit;
            return this;
        }

        public PurchaseOrderItemBuilder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public PurchaseOrderItemBuilder gstPercentage(BigDecimal gstPercentage) {
            this.gstPercentage = gstPercentage;
            return this;
        }

        public PurchaseOrderItemBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PurchaseOrderItem build() {
            return new PurchaseOrderItem(id, purchaseOrder, material, description, quantity, unit, rate, gstPercentage,
                    amount);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public MaterialUnit getUnit() {
        return unit;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(MaterialUnit unit) {
        this.unit = unit;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public void setGstPercentage(BigDecimal gstPercentage) {
        this.gstPercentage = gstPercentage;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
