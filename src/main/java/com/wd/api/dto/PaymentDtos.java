package com.wd.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Payment-related API operations
 */
public class PaymentDtos {

    // ===== Request DTOs =====

    public static class CreateDesignPaymentRequest {
        private Long projectId;
        private String packageName;
        private BigDecimal ratePerSqft;
        private BigDecimal totalSqft;
        private BigDecimal discountPercentage;
        private String paymentType; // 'FULL' or 'INSTALLMENT'

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public BigDecimal getRatePerSqft() {
            return ratePerSqft;
        }

        public void setRatePerSqft(BigDecimal ratePerSqft) {
            this.ratePerSqft = ratePerSqft;
        }

        public BigDecimal getTotalSqft() {
            return totalSqft;
        }

        public void setTotalSqft(BigDecimal totalSqft) {
            this.totalSqft = totalSqft;
        }

        public BigDecimal getDiscountPercentage() {
            return discountPercentage;
        }

        public void setDiscountPercentage(BigDecimal discountPercentage) {
            this.discountPercentage = discountPercentage;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }
    }

    public static class RecordTransactionRequest {
        private BigDecimal amount;
        private String paymentMethod;
        private String referenceNumber;
        private LocalDateTime paymentDate;
        private String notes;

        // TDS and Categorization (Optional, defaults to 0%)
        private BigDecimal tdsPercentage;
        private String tdsDeductedBy;
        private String paymentCategory;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public LocalDateTime getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public BigDecimal getTdsPercentage() {
            return tdsPercentage;
        }

        public void setTdsPercentage(BigDecimal tdsPercentage) {
            this.tdsPercentage = tdsPercentage;
        }

        public String getTdsDeductedBy() {
            return tdsDeductedBy;
        }

        public void setTdsDeductedBy(String tdsDeductedBy) {
            this.tdsDeductedBy = tdsDeductedBy;
        }

        public String getPaymentCategory() {
            return paymentCategory;
        }

        public void setPaymentCategory(String paymentCategory) {
            this.paymentCategory = paymentCategory;
        }
    }

    // ===== Response DTOs =====

    public static class DesignPaymentResponse {
        private Long id;
        private Long projectId;
        private String projectName;
        private String customerName;
        private String packageName;
        private BigDecimal ratePerSqft;
        private BigDecimal totalSqft;
        private BigDecimal baseAmount;
        private BigDecimal gstPercentage;
        private BigDecimal gstAmount;
        private BigDecimal discountPercentage;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String paymentType;
        private String status;
        private BigDecimal totalPaid;
        private BigDecimal balanceDue;
        private LocalDateTime createdAt;
        private List<ScheduleResponse> schedules;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public BigDecimal getRatePerSqft() {
            return ratePerSqft;
        }

        public void setRatePerSqft(BigDecimal ratePerSqft) {
            this.ratePerSqft = ratePerSqft;
        }

        public BigDecimal getTotalSqft() {
            return totalSqft;
        }

        public void setTotalSqft(BigDecimal totalSqft) {
            this.totalSqft = totalSqft;
        }

        public BigDecimal getBaseAmount() {
            return baseAmount;
        }

        public void setBaseAmount(BigDecimal baseAmount) {
            this.baseAmount = baseAmount;
        }

        public BigDecimal getGstPercentage() {
            return gstPercentage;
        }

        public void setGstPercentage(BigDecimal gstPercentage) {
            this.gstPercentage = gstPercentage;
        }

        public BigDecimal getGstAmount() {
            return gstAmount;
        }

        public void setGstAmount(BigDecimal gstAmount) {
            this.gstAmount = gstAmount;
        }

        public BigDecimal getDiscountPercentage() {
            return discountPercentage;
        }

        public void setDiscountPercentage(BigDecimal discountPercentage) {
            this.discountPercentage = discountPercentage;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getTotalPaid() {
            return totalPaid;
        }

        public void setTotalPaid(BigDecimal totalPaid) {
            this.totalPaid = totalPaid;
        }

        public BigDecimal getBalanceDue() {
            return balanceDue;
        }

        public void setBalanceDue(BigDecimal balanceDue) {
            this.balanceDue = balanceDue;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public List<ScheduleResponse> getSchedules() {
            return schedules;
        }

        public void setSchedules(List<ScheduleResponse> schedules) {
            this.schedules = schedules;
        }
    }

    public static class ScheduleResponse {
        private Long id;
        private Integer installmentNumber;
        private String description;
        private BigDecimal amount;
        private LocalDate dueDate;
        private String status;
        private BigDecimal paidAmount;
        private LocalDateTime paidDate;
        private List<TransactionResponse> transactions;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getInstallmentNumber() {
            return installmentNumber;
        }

        public void setInstallmentNumber(Integer installmentNumber) {
            this.installmentNumber = installmentNumber;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getPaidAmount() {
            return paidAmount;
        }

        public void setPaidAmount(BigDecimal paidAmount) {
            this.paidAmount = paidAmount;
        }

        public LocalDateTime getPaidDate() {
            return paidDate;
        }

        public void setPaidDate(LocalDateTime paidDate) {
            this.paidDate = paidDate;
        }

        public List<TransactionResponse> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<TransactionResponse> transactions) {
            this.transactions = transactions;
        }
    }

    public static class TransactionResponse {
        private Long id;
        private String projectName;
        private String customerName;
        private BigDecimal amount;
        private String paymentMethod;
        private String referenceNumber;
        private LocalDateTime paymentDate;
        private String notes;
        private Long recordedById;
        private String receiptNumber;
        private String status;
        private BigDecimal tdsPercentage;
        private BigDecimal tdsAmount;
        private BigDecimal netAmount;
        private String tdsDeductedBy;
        private String paymentCategory;
        private LocalDateTime createdAt;
        private Long challanId;
        private String challanNumber;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public LocalDateTime getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Long getRecordedById() {
            return recordedById;
        }

        public void setRecordedById(Long recordedById) {
            this.recordedById = recordedById;
        }

        public String getReceiptNumber() {
            return receiptNumber;
        }

        public void setReceiptNumber(String receiptNumber) {
            this.receiptNumber = receiptNumber;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public Long getChallanId() {
            return challanId;
        }

        public void setChallanId(Long challanId) {
            this.challanId = challanId;
        }

        public String getChallanNumber() {
            return challanNumber;
        }

        public void setChallanNumber(String challanNumber) {
            this.challanNumber = challanNumber;
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

        public String getTdsDeductedBy() {
            return tdsDeductedBy;
        }

        public void setTdsDeductedBy(String tdsDeductedBy) {
            this.tdsDeductedBy = tdsDeductedBy;
        }

        public String getPaymentCategory() {
            return paymentCategory;
        }

        public void setPaymentCategory(String paymentCategory) {
            this.paymentCategory = paymentCategory;
        }
    }

    // Generic API response wrapper
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    // ===== Phase 2: Retention & GST Invoice DTOs =====

    public static class ReleaseRetentionRequest {
        private BigDecimal releaseAmount;
        private String releaseReason;

        public BigDecimal getReleaseAmount() {
            return releaseAmount;
        }

        public void setReleaseAmount(BigDecimal releaseAmount) {
            this.releaseAmount = releaseAmount;
        }

        public String getReleaseReason() {
            return releaseReason;
        }

        public void setReleaseReason(String releaseReason) {
            this.releaseReason = releaseReason;
        }
    }

    public static class GenerateInvoiceRequest {
        private Long paymentId;
        private String placeOfSupply;
        private String customerGstin;

        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }

        public String getPlaceOfSupply() {
            return placeOfSupply;
        }

        public void setPlaceOfSupply(String placeOfSupply) {
            this.placeOfSupply = placeOfSupply;
        }

        public String getCustomerGstin() {
            return customerGstin;
        }

        public void setCustomerGstin(String customerGstin) {
            this.customerGstin = customerGstin;
        }
    }

    public static class TaxInvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private Long paymentId;
        private String companyGstin;
        private String customerGstin;
        private String placeOfSupply;
        private Boolean isInterstate;
        private BigDecimal taxableValue;
        private BigDecimal cgstRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstRate;
        private BigDecimal sgstAmount;
        private BigDecimal igstRate;
        private BigDecimal igstAmount;
        private BigDecimal totalTaxAmount;
        private BigDecimal invoiceTotal;
        private LocalDate invoiceDate;
        private String financialYear;

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
    }
}
