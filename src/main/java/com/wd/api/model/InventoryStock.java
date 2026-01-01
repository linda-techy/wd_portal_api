package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock")
public class InventoryStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "current_quantity", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentQuantity;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public InventoryStock() {
    }

    public InventoryStock(Long id, CustomerProject project, Material material, BigDecimal currentQuantity,
            LocalDateTime lastUpdated) {
        this.id = id;
        this.project = project;
        this.material = material;
        this.currentQuantity = currentQuantity;
        this.lastUpdated = lastUpdated;
    }

    public static InventoryStockBuilder builder() {
        return new InventoryStockBuilder();
    }

    public static class InventoryStockBuilder {
        private Long id;
        private CustomerProject project;
        private Material material;
        private BigDecimal currentQuantity;
        private LocalDateTime lastUpdated;

        public InventoryStockBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InventoryStockBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public InventoryStockBuilder material(Material material) {
            this.material = material;
            return this;
        }

        public InventoryStockBuilder currentQuantity(BigDecimal currentQuantity) {
            this.currentQuantity = currentQuantity;
            return this;
        }

        public InventoryStockBuilder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public InventoryStock build() {
            return new InventoryStock(id, project, material, currentQuantity, lastUpdated);
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
}
