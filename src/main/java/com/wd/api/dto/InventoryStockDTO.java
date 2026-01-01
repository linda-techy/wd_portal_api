package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryStockDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long materialId;
    private String materialName;
    private String unit;
    private BigDecimal currentQuantity;
    private LocalDateTime lastUpdated;

    public InventoryStockDTO() {
    }

    public InventoryStockDTO(Long id, Long projectId, String projectName, Long materialId, String materialName,
            String unit, BigDecimal currentQuantity, LocalDateTime lastUpdated) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.materialId = materialId;
        this.materialName = materialName;
        this.unit = unit;
        this.currentQuantity = currentQuantity;
        this.lastUpdated = lastUpdated;
    }

    public static InventoryStockDTOBuilder builder() {
        return new InventoryStockDTOBuilder();
    }

    public static class InventoryStockDTOBuilder {
        private Long id;
        private Long projectId;
        private String projectName;
        private Long materialId;
        private String materialName;
        private String unit;
        private BigDecimal currentQuantity;
        private LocalDateTime lastUpdated;

        public InventoryStockDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public InventoryStockDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public InventoryStockDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public InventoryStockDTOBuilder materialId(Long materialId) {
            this.materialId = materialId;
            return this;
        }

        public InventoryStockDTOBuilder materialName(String materialName) {
            this.materialName = materialName;
            return this;
        }

        public InventoryStockDTOBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public InventoryStockDTOBuilder currentQuantity(BigDecimal currentQuantity) {
            this.currentQuantity = currentQuantity;
            return this;
        }

        public InventoryStockDTOBuilder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public InventoryStockDTO build() {
            return new InventoryStockDTO(id, projectId, projectName, materialId, materialName, unit, currentQuantity,
                    lastUpdated);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
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
