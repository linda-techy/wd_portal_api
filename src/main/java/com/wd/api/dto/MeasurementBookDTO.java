package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MeasurementBookDTO {
    private Long id;
    private Long projectId;
    private Long labourId;
    private Long boqItemId;
    private String description;
    private LocalDate measurementDate;
    private BigDecimal length;
    private BigDecimal breadth;
    private BigDecimal depth;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal rate;
    private BigDecimal totalAmount;

    public MeasurementBookDTO() {
    }

    public MeasurementBookDTO(Long id, Long projectId, Long labourId, Long boqItemId, String description,
            LocalDate measurementDate, BigDecimal length, BigDecimal breadth, BigDecimal depth, BigDecimal quantity,
            String unit, BigDecimal rate, BigDecimal totalAmount) {
        this.id = id;
        this.projectId = projectId;
        this.labourId = labourId;
        this.boqItemId = boqItemId;
        this.description = description;
        this.measurementDate = measurementDate;
        this.length = length;
        this.breadth = breadth;
        this.depth = depth;
        this.quantity = quantity;
        this.unit = unit;
        this.rate = rate;
        this.totalAmount = totalAmount;
    }

    public static MeasurementBookDTOBuilder builder() {
        return new MeasurementBookDTOBuilder();
    }

    public static class MeasurementBookDTOBuilder {
        private Long id;
        private Long projectId;
        private Long labourId;
        private Long boqItemId;
        private String description;
        private LocalDate measurementDate;
        private BigDecimal length;
        private BigDecimal breadth;
        private BigDecimal depth;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal totalAmount;

        public MeasurementBookDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MeasurementBookDTOBuilder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public MeasurementBookDTOBuilder labourId(Long labourId) {
            this.labourId = labourId;
            return this;
        }

        public MeasurementBookDTOBuilder boqItemId(Long boqItemId) {
            this.boqItemId = boqItemId;
            return this;
        }

        public MeasurementBookDTOBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MeasurementBookDTOBuilder measurementDate(LocalDate measurementDate) {
            this.measurementDate = measurementDate;
            return this;
        }

        public MeasurementBookDTOBuilder length(BigDecimal length) {
            this.length = length;
            return this;
        }

        public MeasurementBookDTOBuilder breadth(BigDecimal breadth) {
            this.breadth = breadth;
            return this;
        }

        public MeasurementBookDTOBuilder depth(BigDecimal depth) {
            this.depth = depth;
            return this;
        }

        public MeasurementBookDTOBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public MeasurementBookDTOBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public MeasurementBookDTOBuilder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public MeasurementBookDTOBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public MeasurementBookDTO build() {
            return new MeasurementBookDTO(id, projectId, labourId, boqItemId, description, measurementDate, length,
                    breadth, depth, quantity, unit, rate, totalAmount);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getLabourId() {
        return labourId;
    }

    public Long getBoqItemId() {
        return boqItemId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getMeasurementDate() {
        return measurementDate;
    }

    public BigDecimal getLength() {
        return length;
    }

    public BigDecimal getBreadth() {
        return breadth;
    }

    public BigDecimal getDepth() {
        return depth;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setLabourId(Long labourId) {
        this.labourId = labourId;
    }

    public void setBoqItemId(Long boqItemId) {
        this.boqItemId = boqItemId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public void setBreadth(BigDecimal breadth) {
        this.breadth = breadth;
    }

    public void setDepth(BigDecimal depth) {
        this.depth = depth;
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

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
