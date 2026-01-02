package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subcontract_payments")
public class SubcontractPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private SubcontractWorkOrder workOrder;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "tds_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal tdsPercentage = new BigDecimal("1.00");

    @Column(name = "tds_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal tdsAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "payment_mode", nullable = false)
    private String paymentMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SubcontractWorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(SubcontractWorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getTdsPercentage() {
        return tdsPercentage;
    }

    public void setTdsPercentage(BigDecimal tdsPercentage) {
        this.tdsPercentage = tdsPercentage;
    }

    public BigDecimal getTdsAmount() {
        return tdsAmount;
    }

    public void setTdsAmount(BigDecimal tdsAmount) {
        this.tdsAmount = tdsAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
