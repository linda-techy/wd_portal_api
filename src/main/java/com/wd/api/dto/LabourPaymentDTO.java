package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LabourPaymentDTO {
    private Long id;
    private Long labourId;
    private String labourName;
    private Long projectId;
    private String projectName;
    private Long mbEntryId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String notes;

    public LabourPaymentDTO() {
    }

    public LabourPaymentDTO(Long id, Long labourId, String labourName, Long projectId, String projectName,
            Long mbEntryId, BigDecimal amount, LocalDate paymentDate, String paymentMethod, String notes) {
        this.id = id;
        this.labourId = labourId;
        this.labourName = labourName;
        this.projectId = projectId;
        this.projectName = projectName;
        this.mbEntryId = mbEntryId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    public static LabourPaymentDTOBuilder builder() {
        return new LabourPaymentDTOBuilder();
    }

    public static class LabourPaymentDTOBuilder {
        private Long id;
        private Long labourId;
        private String labourName;
        private Long projectId;
        private String projectName;
        private Long mbEntryId;
        private BigDecimal amount;
        private LocalDate paymentDate;
        private String paymentMethod;
        private String notes;

        public LabourPaymentDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabourPaymentDTOBuilder labourId(Long labourId) {
            this.labourId = labourId;
            return this;
        }

        public LabourPaymentDTOBuilder labourName(String labourName) {
            this.labourName = labourName;
            return this;
        }

        public LabourPaymentDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public LabourPaymentDTOBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public LabourPaymentDTOBuilder mbEntryId(Long mbEntryId) {
            this.mbEntryId = mbEntryId;
            return this;
        }

        public LabourPaymentDTOBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public LabourPaymentDTOBuilder paymentDate(LocalDate paymentDate) {
            this.paymentDate = paymentDate;
            return this;
        }

        public LabourPaymentDTOBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public LabourPaymentDTOBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public LabourPaymentDTO build() {
            return new LabourPaymentDTO(id, labourId, labourName, projectId, projectName, mbEntryId, amount,
                    paymentDate, paymentMethod, notes);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getLabourId() {
        return labourId;
    }

    public String getLabourName() {
        return labourName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getMbEntryId() {
        return mbEntryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
