package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Individual line on a BOQ Invoice.
 * line_type values: STAGE_AMOUNT, CO_AMOUNT, GST, CREDIT_NOTE_DEDUCTION, RETENTION
 */
@Entity
@Table(name = "boq_invoice_line_items")
public class BoqInvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private BoqInvoice invoice;

    @Column(name = "line_type", nullable = false, length = 30)
    private String lineType;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (quantity == null) quantity = BigDecimal.ONE;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        if (amount == null) amount = BigDecimal.ZERO;
        if (sortOrder == null) sortOrder = 0;
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BoqInvoice getInvoice() { return invoice; }
    public void setInvoice(BoqInvoice invoice) { this.invoice = invoice; }

    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
