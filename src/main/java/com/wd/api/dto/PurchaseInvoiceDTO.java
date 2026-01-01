package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchaseInvoiceDTO {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private Long projectId;
    private String projectName;
    private Long poId;
    private Long grnId;
    private String vendorInvoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private String status;

    public PurchaseInvoiceDTO() {
    }

    public PurchaseInvoiceDTO(Long id, Long vendorId, String vendorName, Long projectId, String projectName, Long poId,
            Long grnId, String vendorInvoiceNumber, LocalDate invoiceDate, BigDecimal amount, String status) {
        this.id = id;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.projectId = projectId;
        this.projectName = projectName;
        this.poId = poId;
        this.grnId = grnId;
        this.vendorInvoiceNumber = vendorInvoiceNumber;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.status = status;
    }

    public static PurchaseInvoiceDTOBuilder builder() {
        return new PurchaseInvoiceDTOBuilder();
    }

    public static class PurchaseInvoiceDTOBuilder {
        private Long id;
        private Long vendorId;
        private String vendorName;
        private Long projectId;
        private String projectName;
        private Long poId;
        private Long grnId;
        private String vendorInvoiceNumber;
        private LocalDate invoiceDate;
        private BigDecimal amount;
        private String status;

        public PurchaseInvoiceDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PurchaseInvoiceDTOBuilder vendorId(Long vendorId) {
            this.vendorId = vendorId;
            return this;
        }

        public PurchaseInvoiceDTOBuilder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public PurchaseInvoiceDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public PurchaseInvoiceDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public PurchaseInvoiceDTOBuilder poId(Long poId) {
            this.poId = poId;
            return this;
        }

        public PurchaseInvoiceDTOBuilder grnId(Long grnId) {
            this.grnId = grnId;
            return this;
        }

        public PurchaseInvoiceDTOBuilder vendorInvoiceNumber(String vendorInvoiceNumber) {
            this.vendorInvoiceNumber = vendorInvoiceNumber;
            return this;
        }

        public PurchaseInvoiceDTOBuilder invoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public PurchaseInvoiceDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PurchaseInvoiceDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PurchaseInvoiceDTO build() {
            return new PurchaseInvoiceDTO(id, vendorId, vendorName, projectId, projectName, poId, grnId,
                    vendorInvoiceNumber, invoiceDate, amount, status);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getPoId() {
        return poId;
    }

    public Long getGrnId() {
        return grnId;
    }

    public String getVendorInvoiceNumber() {
        return vendorInvoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
