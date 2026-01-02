package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", unique = true, nullable = false, length = 50)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "gst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // 'DRAFT', 'ISSUED', 'RECEIVED', 'CANCELLED'

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = "DRAFT";
    }

    public PurchaseOrder() {
    }

    public PurchaseOrder(Long id, String poNumber, CustomerProject project, Vendor vendor, LocalDate poDate,
            LocalDate expectedDeliveryDate, BigDecimal totalAmount, BigDecimal gstAmount, BigDecimal netAmount,
            String status, String notes, Long createdById, LocalDateTime createdAt, List<PurchaseOrderItem> items) {
        this.id = id;
        this.poNumber = poNumber;
        this.project = project;
        this.vendor = vendor;
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

    public static PurchaseOrderBuilder builder() {
        return new PurchaseOrderBuilder();
    }

    public static class PurchaseOrderBuilder {
        private Long id;
        private String poNumber;
        private CustomerProject project;
        private Vendor vendor;
        private LocalDate poDate;
        private LocalDate expectedDeliveryDate;
        private BigDecimal totalAmount;
        private BigDecimal gstAmount;
        private BigDecimal netAmount;
        private String status = "DRAFT";
        private String notes;
        private Long createdById;
        private LocalDateTime createdAt;
        private List<PurchaseOrderItem> items;

        public PurchaseOrderBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PurchaseOrderBuilder poNumber(String poNumber) {
            this.poNumber = poNumber;
            return this;
        }

        public PurchaseOrderBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public PurchaseOrderBuilder vendor(Vendor vendor) {
            this.vendor = vendor;
            return this;
        }

        public PurchaseOrderBuilder poDate(LocalDate poDate) {
            this.poDate = poDate;
            return this;
        }

        public PurchaseOrderBuilder expectedDeliveryDate(LocalDate expectedDeliveryDate) {
            this.expectedDeliveryDate = expectedDeliveryDate;
            return this;
        }

        public PurchaseOrderBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public PurchaseOrderBuilder gstAmount(BigDecimal gstAmount) {
            this.gstAmount = gstAmount;
            return this;
        }

        public PurchaseOrderBuilder netAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
            return this;
        }

        public PurchaseOrderBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PurchaseOrderBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public PurchaseOrderBuilder createdById(Long createdById) {
            this.createdById = createdById;
            return this;
        }

        public PurchaseOrderBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PurchaseOrderBuilder items(List<PurchaseOrderItem> items) {
            this.items = items;
            return this;
        }

        public PurchaseOrder build() {
            return new PurchaseOrder(id, poNumber, project, vendor, poDate, expectedDeliveryDate, totalAmount,
                    gstAmount, netAmount, status, notes, createdById, createdAt, items);
        }
    }

    public Long getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public CustomerProject getProject() {
        return project;
    }

    public Vendor getVendor() {
        return vendor;
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

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
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

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
