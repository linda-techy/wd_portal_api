package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "measurement_book")
public class MeasurementBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "labour_id")
    private Labour labour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_item_id")
    private BoqItem boqItem;

    @Column(nullable = false)
    private String description;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal breadth;

    @Column(precision = 10, scale = 2)
    private BigDecimal depth; // Height/Depth

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit;

    @Column(precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public MeasurementBook() {
    }

    public MeasurementBook(Long id, CustomerProject project, Labour labour, BoqItem boqItem, String description,
            LocalDate measurementDate, BigDecimal length, BigDecimal breadth, BigDecimal depth, BigDecimal quantity,
            String unit, BigDecimal rate, BigDecimal totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.project = project;
        this.labour = labour;
        this.boqItem = boqItem;
        this.description = description;
        this.measurementDate = measurementDate;
        this.length = length;
        this.breadth = breadth;
        this.depth = depth;
        this.quantity = quantity;
        this.unit = unit;
        this.rate = rate;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public static MeasurementBookBuilder builder() {
        return new MeasurementBookBuilder();
    }

    public static class MeasurementBookBuilder {
        private Long id;
        private CustomerProject project;
        private Labour labour;
        private BoqItem boqItem;
        private String description;
        private LocalDate measurementDate;
        private BigDecimal length;
        private BigDecimal breadth;
        private BigDecimal depth;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal totalAmount;
        private LocalDateTime createdAt;

        public MeasurementBookBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public MeasurementBookBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public MeasurementBookBuilder labour(Labour labour) {
            this.labour = labour;
            return this;
        }

        public MeasurementBookBuilder boqItem(BoqItem boqItem) {
            this.boqItem = boqItem;
            return this;
        }

        public MeasurementBookBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MeasurementBookBuilder measurementDate(LocalDate measurementDate) {
            this.measurementDate = measurementDate;
            return this;
        }

        public MeasurementBookBuilder length(BigDecimal length) {
            this.length = length;
            return this;
        }

        public MeasurementBookBuilder breadth(BigDecimal breadth) {
            this.breadth = breadth;
            return this;
        }

        public MeasurementBookBuilder depth(BigDecimal depth) {
            this.depth = depth;
            return this;
        }

        public MeasurementBookBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public MeasurementBookBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public MeasurementBookBuilder rate(BigDecimal rate) {
            this.rate = rate;
            return this;
        }

        public MeasurementBookBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public MeasurementBookBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MeasurementBook build() {
            return new MeasurementBook(id, project, labour, boqItem, description, measurementDate, length, breadth,
                    depth, quantity, unit, rate, totalAmount, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerProject getProject() {
        return project;
    }

    public Labour getLabour() {
        return labour;
    }

    public BoqItem getBoqItem() {
        return boqItem;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public void setLabour(Labour labour) {
        this.labour = labour;
    }

    public void setBoqItem(BoqItem boqItem) {
        this.boqItem = boqItem;
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
