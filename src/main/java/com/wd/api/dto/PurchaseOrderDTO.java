package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseOrderDTO {
    private Long id;
    private String poNumber;
    private Long projectId;
    private String projectName;
    private Long vendorId;
    private String vendorName;
    private LocalDate poDate;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private BigDecimal gstAmount;
    private BigDecimal netAmount;
    private String status;
    private String notes;
    private Long createdById;
    private LocalDateTime createdAt;
    private List<PurchaseOrderItemDTO> items;

    public PurchaseOrderDTO() {
    }

    public PurchaseOrderDTO(Long id, String poNumber, Long projectId, String projectName, Long vendorId,
            String vendorName, LocalDate poDate, LocalDate expectedDeliveryDate, BigDecimal totalAmount,
            BigDecimal gstAmount, BigDecimal netAmount, String status, String notes, Long createdById,
            LocalDateTime createdAt, List<PurchaseOrderItemDTO> items) {
        this.id = id;
        this.poNumber = poNumber;
        this.projectId = projectId;
        this.projectName = projectName;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.poDate = poDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.totalAmount = totalAmount;
        this.gstAmount = gstAmount;
        this.netAmount = netAmount;
        this.status = status;
        this.notes = notes;
        this.createdById = createdById;
        this.createdAt = createdAt;
        this.items = items;
    }

    public static PurchaseOrderDTOBuilder builder() {
        return new PurchaseOrderDTOBuilder();
    }

    public static class PurchaseOrderDTOBuilder {
        private Long id;
        private String poNumber;
        private Long projectId;
        private String projectName;
        private Long vendorId;
        private String vendorName;
        private LocalDate poDate;
        private LocalDate expectedDeliveryDate;
        private BigDecimal totalAmount;
        private BigDecimal gstAmount;
        private BigDecimal netAmount;
        private String status;
        private String notes;
        private Long createdById;
        private LocalDateTime createdAt;
        private List<PurchaseOrderItemDTO> items;

        public PurchaseOrderDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PurchaseOrderDTOBuilder poNumber(String poNumber) {
            this.poNumber = poNumber;
            return this;
        }

        public PurchaseOrderDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public PurchaseOrderDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public PurchaseOrderDTOBuilder vendorId(Long vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public PurchaseOrderDTOBuilder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public PurchaseOrderDTOBuilder poDate(LocalDate poDate) {
            this.poDate = poDate;
            return this;
        }

        public PurchaseOrderDTOBuilder expectedDeliveryDate(LocalDate expectedDeliveryDate) {
            this.expectedDeliveryDate = expectedDeliveryDate;
            return this;
        }

        public PurchaseOrderDTOBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public PurchaseOrderDTOBuilder gstAmount(BigDecimal gstAmount) {
            this.gstAmount = gstAmount;
            return this;
        }

        public PurchaseOrderDTOBuilder netAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public PurchaseOrderDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PurchaseOrderDTOBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public PurchaseOrderDTOBuilder createdById(Long createdById) {
            this.createdById = createdById;
            return this;
        }

        public PurchaseOrderDTOBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PurchaseOrderDTOBuilder items(List<PurchaseOrderItemDTO> items) {
            this.items = items;
            return this;
        }

        public PurchaseOrderDTO build() {
            return new PurchaseOrderDTO(id, poNumber, projectId, projectName, vendorId, vendorName, poDate,
                    expectedDeliveryDate, totalAmount, gstAmount, netAmount, status, notes, createdById, createdAt,
                    items);
        }
    }

    public Long getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public LocalDate getPoDate() {
        return poDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PurchaseOrderItemDTO> getItems() {
        return items;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setPoDate(LocalDate poDate) {
        this.poDate = poDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setItems(List<PurchaseOrderItemDTO> items) {
        this.items = items;
    }
}
