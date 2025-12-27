package com.wd.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_invoices")
public class TaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    // GST Details
    @Column(name = "company_gstin", nullable = false, length = 15)
    private String companyGstin = "29AABCU9603R1ZX"; // Default placeholder

    @Column(name = "customer_gstin", length = 15)
    private String customerGstin;

    @Column(name = "place_of_supply", nullable = false)
    private String placeOfSupply;

    @Column(name = "is_interstate", nullable = false)
    private Boolean isInterstate = false;

    // Financial Breakdown
    @Column(name = "taxable_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableValue;

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "cgst_amount", precision = 15, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "sgst_amount", precision = 15, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Column(name = "igst_amount", precision = 15, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "total_tax_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalTaxAmount;

    @Column(name = "invoice_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal invoiceTotal;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "financial_year", length = 10, nullable = false)
    private String financialYear;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (invoiceDate == null) {
            invoiceDate = LocalDate.now();
        }
        calculateFinancialYear();
    }

    private void calculateFinancialYear() {
        int month = invoiceDate.getMonthValue();
        int year = invoiceDate.getYear();

        if (month >= 4) {
            // April-December: FY is current year to next year
            this.financialYear = year + "-" + String.format("%02d", (year + 1) % 100);
        } else {
            // January-March: FY is previous year to current year
            this.financialYear = (year - 1) + "-" + String.format("%02d", year % 100);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getCompanyGstin() {
        return companyGstin;
    }

    public void setCompanyGstin(String companyGstin) {
        this.companyGstin = companyGstin;
    }

    public String getCustomerGstin() {
        return customerGstin;
    }

    public void setCustomerGstin(String customerGstin) {
        this.customerGstin = customerGstin;
    }

    public String getPlaceOfSupply() {
        return placeOfSupply;
    }

    public void setPlaceOfSupply(String placeOfSupply) {
        this.placeOfSupply = placeOfSupply;
    }

    public Boolean getIsInterstate() {
        return isInterstate;
    }

    public void setIsInterstate(Boolean isInterstate) {
        this.isInterstate = isInterstate;
    }

    public BigDecimal getTaxableValue() {
        return taxableValue;
    }

    public void setTaxableValue(BigDecimal taxableValue) {
        this.taxableValue = taxableValue;
    }

    public BigDecimal getCgstRate() {
        return cgstRate;
    }

    public void setCgstRate(BigDecimal cgstRate) {
        this.cgstRate = cgstRate;
    }

    public BigDecimal getCgstAmount() {
        return cgstAmount;
    }

    public void setCgstAmount(BigDecimal cgstAmount) {
        this.cgstAmount = cgstAmount;
    }

    public BigDecimal getSgstRate() {
        return sgstRate;
    }

    public void setSgstRate(BigDecimal sgstRate) {
        this.sgstRate = sgstRate;
    }

    public BigDecimal getSgstAmount() {
        return sgstAmount;
    }

    public void setSgstAmount(BigDecimal sgstAmount) {
        this.sgstAmount = sgstAmount;
    }

    public BigDecimal getIgstRate() {
        return igstRate;
    }

    public void setIgstRate(BigDecimal igstRate) {
        this.igstRate = igstRate;
    }

    public BigDecimal getIgstAmount() {
        return igstAmount;
    }

    public void setIgstAmount(BigDecimal igstAmount) {
        this.igstAmount = igstAmount;
    }

    public BigDecimal getTotalTaxAmount() {
        return totalTaxAmount;
    }

    public void setTotalTaxAmount(BigDecimal totalTaxAmount) {
        this.totalTaxAmount = totalTaxAmount;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public void setInvoiceTotal(BigDecimal invoiceTotal) {
        this.invoiceTotal = invoiceTotal;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }
}
