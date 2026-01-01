package com.wd.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GRNDTO {
    private Long id;
    private String grnNumber;
    private Long poId;
    private String poNumber;
    private LocalDateTime receivedDate;
    private Long receivedById;
    private String receivedByName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String challanNumber;
    private String notes;

    public GRNDTO() {
    }

    public GRNDTO(Long id, String grnNumber, Long poId, String poNumber, LocalDateTime receivedDate, Long receivedById,
            String receivedByName, String invoiceNumber, LocalDate invoiceDate, String challanNumber, String notes) {
        this.id = id;
        this.grnNumber = grnNumber;
        this.poId = poId;
        this.poNumber = poNumber;
        this.receivedDate = receivedDate;
        this.receivedById = receivedById;
        this.receivedByName = receivedByName;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.challanNumber = challanNumber;
        this.notes = notes;
    }

    public static GRNDTOBuilder builder() {
        return new GRNDTOBuilder();
    }

    public static class GRNDTOBuilder {
        private Long id;
        private String grnNumber;
        private Long poId;
        private String poNumber;
        private LocalDateTime receivedDate;
        private Long receivedById;
        private String receivedByName;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private String challanNumber;
        private String notes;

        public GRNDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GRNDTOBuilder grnNumber(String grnNumber) {
            this.grnNumber = grnNumber;
            return this;
        }

        public GRNDTOBuilder poId(Long poId) {
            this.poId = poId;
            return this;
        }

        public GRNDTOBuilder poNumber(String poNumber) {
            this.poNumber = poNumber;
            return this;
        }

        public GRNDTOBuilder receivedDate(LocalDateTime receivedDate) {
            this.receivedDate = receivedDate;
            return this;
        }

        public GRNDTOBuilder receivedById(Long receivedById) {
            this.receivedById = receivedById;
            return this;
        }

        public GRNDTOBuilder receivedByName(String receivedByName) {
            this.receivedByName = receivedByName;
            return this;
        }

        public GRNDTOBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public GRNDTOBuilder invoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public GRNDTOBuilder challanNumber(String challanNumber) {
            this.challanNumber = challanNumber;
            return this;
        }

        public GRNDTOBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public GRNDTO build() {
            return new GRNDTO(id, grnNumber, poId, poNumber, receivedDate, receivedById, receivedByName, invoiceNumber,
                    invoiceDate, challanNumber, notes);
        }
    }

    public Long getId() {
        return id;
    }

    public String getGrnNumber() {
        return grnNumber;
    }

    public Long getPoId() {
        return poId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public Long getReceivedById() {
        return receivedById;
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public String getChallanNumber() {
        return challanNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGrnNumber(String grnNumber) {
        this.grnNumber = grnNumber;
    }

    public void setPoId(Long poId) {
        this.poId = poId;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public void setReceivedById(Long receivedById) {
        this.receivedById = receivedById;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public void setChallanNumber(String challanNumber) {
        this.challanNumber = challanNumber;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
