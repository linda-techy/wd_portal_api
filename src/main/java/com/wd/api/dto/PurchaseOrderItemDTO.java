package com.wd.api.dto;

import java.math.BigDecimal;

public class PurchaseOrderItemDTO {
    private Long id;
    private String description;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal rate;
    private BigDecimal gstPercentage;
    private BigDecimal amount;
    private Long materialId;

    public PurchaseOrderItemDTO() {
    }

    public PurchaseOrderItemDTO(Long id, String description, BigDecimal quantity, String unit, BigDecimal rate,
            BigDecimal gstPercentage, BigDecimal amount, Long materialId) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.rate = rate;
        this.gstPercentage = gstPercentage;
        this.amount = amount;
        this.materialId = materialId;
    }

    public static PurchaseOrderItemDTOBuilder builder() {
        return new PurchaseOrderItemDTOBuilder();
    }

    public static class PurchaseOrderItemDTOBuilder {
        private Long id;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal gstPercentage;
        private BigDecimal amount;
        private Long materialId;

        public PurchaseOrderItemDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PurchaseOrderItemDTOBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PurchaseOrderItemDTOBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public PurchaseOrderItemDTOBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public PurchaseOrderItemDTOBuilder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public PurchaseOrderItemDTOBuilder gstPercentage(BigDecimal gstPercentage) {
            this.gstPercentage = gstPercentage;
            return this;
        }

        public PurchaseOrderItemDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PurchaseOrderItemDTOBuilder materialId(Long materialId) {
            this.materialId = materialId;
            return this;
        }

        public PurchaseOrderItemDTO build() {
            return new PurchaseOrderItemDTO(id, description, quantity, unit, rate, gstPercentage, amount, materialId);
        }
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
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

    public Long getMaterialId() {
        return materialId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
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

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }
}
