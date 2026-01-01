package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectInvoiceDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal subTotal;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String status;
    private String notes;

    public ProjectInvoiceDTO() {
    }

    public ProjectInvoiceDTO(Long id, Long projectId, String projectName, String invoiceNumber, LocalDate invoiceDate,
            LocalDate dueDate, BigDecimal subTotal, BigDecimal gstPercentage, BigDecimal gstAmount,
            BigDecimal totalAmount, String status, String notes) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.subTotal = subTotal;
        this.gstPercentage = gstPercentage;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.notes = notes;
    }

    public static ProjectInvoiceDTOBuilder builder() {
        return new ProjectInvoiceDTOBuilder();
    }

    public static class ProjectInvoiceDTOBuilder {
        private Long id;
        private Long projectId;
        private String projectName;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private BigDecimal subTotal;
        private BigDecimal gstPercentage;
        private BigDecimal gstAmount;
        private BigDecimal totalAmount;
        private String status;
        private String notes;

        public ProjectInvoiceDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ProjectInvoiceDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public ProjectInvoiceDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public ProjectInvoiceDTOBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public ProjectInvoiceDTOBuilder invoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public ProjectInvoiceDTOBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public ProjectInvoiceDTOBuilder subTotal(BigDecimal subTotal) {
            this.subTotal = subTotal;
            return this;
        }

        public ProjectInvoiceDTOBuilder gstPercentage(BigDecimal gstPercentage) {
            this.gstPercentage = gstPercentage;
            return this;
        }

        public ProjectInvoiceDTOBuilder gstAmount(BigDecimal gstAmount) {
            this.gstAmount = gstAmount;
            return this;
        }

        public ProjectInvoiceDTOBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public ProjectInvoiceDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ProjectInvoiceDTOBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public ProjectInvoiceDTO build() {
            return new ProjectInvoiceDTO(id, projectId, projectName, invoiceNumber, invoiceDate, dueDate, subTotal,
                    gstPercentage, gstAmount, totalAmount, status, notes);
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
