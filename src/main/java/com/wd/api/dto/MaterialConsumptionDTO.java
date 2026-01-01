package com.wd.api.dto;

import java.math.BigDecimal;

public class MaterialConsumptionDTO {
    private Long materialId;
    private String materialName;
    private String unit;
    private BigDecimal totalPurchased;
    private BigDecimal currentStock;
    private BigDecimal totalWastage;
    private BigDecimal totalTheft;
    private BigDecimal totalDamage;
    private BigDecimal impliedConsumption;

    public MaterialConsumptionDTO() {
    }

    public MaterialConsumptionDTO(Long materialId, String materialName, String unit, BigDecimal totalPurchased,
            BigDecimal currentStock, BigDecimal totalWastage, BigDecimal totalTheft, BigDecimal totalDamage,
            BigDecimal impliedConsumption) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.unit = unit;
        this.totalPurchased = totalPurchased;
        this.currentStock = currentStock;
        this.totalWastage = totalWastage;
        this.totalTheft = totalTheft;
        this.totalDamage = totalDamage;
        this.impliedConsumption = impliedConsumption;
    }

    public static MaterialConsumptionDTOBuilder builder() {
        return new MaterialConsumptionDTOBuilder();
    }

    public static class MaterialConsumptionDTOBuilder {
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal totalPurchased;
        private BigDecimal currentStock;
        private BigDecimal totalWastage;
        private BigDecimal totalTheft;
        private BigDecimal totalDamage;
        private BigDecimal impliedConsumption;

        public MaterialConsumptionDTOBuilder materialId(Long materialId) {
            this.materialId = materialId;
            return this;
        }

        public MaterialConsumptionDTOBuilder materialName(String materialName) {
            this.materialName = materialName;
            return this;
        }

        public MaterialConsumptionDTOBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public MaterialConsumptionDTOBuilder totalPurchased(BigDecimal totalPurchased) {
            this.totalPurchased = totalPurchased;
            return this;
        }

        public MaterialConsumptionDTOBuilder currentStock(BigDecimal currentStock) {
            this.currentStock = currentStock;
            return this;
        }

        public MaterialConsumptionDTOBuilder totalWastage(BigDecimal totalWastage) {
            this.totalWastage = totalWastage;
            return this;
        }

        public MaterialConsumptionDTOBuilder totalTheft(BigDecimal totalTheft) {
            this.totalTheft = totalTheft;
            return this;
        }

        public MaterialConsumptionDTOBuilder totalDamage(BigDecimal totalDamage) {
            this.totalDamage = totalDamage;
            return this;
        }

        public MaterialConsumptionDTOBuilder impliedConsumption(BigDecimal impliedConsumption) {
            this.impliedConsumption = impliedConsumption;
            return this;
        }

        public MaterialConsumptionDTO build() {
            return new MaterialConsumptionDTO(materialId, materialName, unit, totalPurchased, currentStock,
                    totalWastage, totalTheft, totalDamage, impliedConsumption);
        }
    }

    public Long getMaterialId() {
        return materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getTotalPurchased() {
        return totalPurchased;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public BigDecimal getTotalWastage() {
        return totalWastage;
    }

    public BigDecimal getTotalTheft() {
        return totalTheft;
    }

    public BigDecimal getTotalDamage() {
        return totalDamage;
    }

    public BigDecimal getImpliedConsumption() {
        return impliedConsumption;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setTotalPurchased(BigDecimal totalPurchased) {
        this.totalPurchased = totalPurchased;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public void setTotalWastage(BigDecimal totalWastage) {
        this.totalWastage = totalWastage;
    }

    public void setTotalTheft(BigDecimal totalTheft) {
        this.totalTheft = totalTheft;
    }

    public void setTotalDamage(BigDecimal totalDamage) {
        this.totalDamage = totalDamage;
    }

    public void setImpliedConsumption(BigDecimal impliedConsumption) {
        this.impliedConsumption = impliedConsumption;
    }
}
