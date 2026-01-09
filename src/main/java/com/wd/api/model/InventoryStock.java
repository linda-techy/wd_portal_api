package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_stock")
public class InventoryStock extends BaseEntity {

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

    public InventoryStock() {
    }

    public InventoryStock(Long id, CustomerProject project, Material material, BigDecimal currentQuantity) {
        this.id = id;
        this.project = project;
        this.material = material;
        this.currentQuantity = currentQuantity;
    }

    public static InventoryStockBuilder builder() {
        return new InventoryStockBuilder();
    }

    public static class InventoryStockBuilder {
        private Long id;
        private CustomerProject project;
        private Material material;
        private BigDecimal currentQuantity;

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

        public InventoryStock build() {
            return new InventoryStock(id, project, material, currentQuantity);
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

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
}
