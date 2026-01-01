package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_invoices")
public class ProjectInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CustomerProject project;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sub_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal subTotal;

    @Column(name = "gst_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal gstPercentage = new BigDecimal("18.00");

    @Column(name = "gst_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal gstAmount;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT, ISSUED, PAID, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = "DRAFT";
    }

    public ProjectInvoice() {
    }

    public ProjectInvoice(Long id, CustomerProject project, String invoiceNumber, LocalDate invoiceDate,
            LocalDate dueDate, BigDecimal subTotal, BigDecimal gstPercentage, BigDecimal gstAmount,
            BigDecimal totalAmount, String status, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.project = project;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.subTotal = subTotal;
        this.gstPercentage = gstPercentage;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static ProjectInvoiceBuilder builder() {
        return new ProjectInvoiceBuilder();
    }

    public static class ProjectInvoiceBuilder {
        private Long id;
        private CustomerProject project;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private BigDecimal subTotal;
        private BigDecimal gstPercentage;
        private BigDecimal gstAmount;
        private BigDecimal totalAmount;
        private String status;
        private String notes;
        private LocalDateTime createdAt;

        public ProjectInvoiceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ProjectInvoiceBuilder project(CustomerProject project) {
            this.project = project;
            return this;
        }

        public ProjectInvoiceBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public ProjectInvoiceBuilder invoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public ProjectInvoiceBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public ProjectInvoiceBuilder subTotal(BigDecimal subTotal) {
            this.subTotal = subTotal;
            return this;
        }

        public ProjectInvoiceBuilder gstPercentage(BigDecimal gstPercentage) {
            this.gstPercentage = gstPercentage;
            return this;
        }

        public ProjectInvoiceBuilder gstAmount(BigDecimal gstAmount) {
            this.gstAmount = gstAmount;
            return this;
        }

        public ProjectInvoiceBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public ProjectInvoiceBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ProjectInvoiceBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public ProjectInvoiceBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProjectInvoice build() {
            return new ProjectInvoice(id, project, invoiceNumber, invoiceDate, dueDate, subTotal, gstPercentage,
                    gstAmount, totalAmount, status, notes, createdAt);
        }
    }

    public Long getId() {
        return id;
    }

    public CustomerProject getProject() {
        return project;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
