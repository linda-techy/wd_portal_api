package com.wd.api.dto;

import com.wd.api.model.PaymentTransaction;
import java.math.BigDecimal;

/**
 * Customer-safe DTO for Payment Transactions.
 * Shows only customer-relevant payment information.
 * Excludes:
 * - TDS details (company tax strategy)
 * - Internal employee IDs
 * - Internal accounting references
 * - Net amounts (reveals company margins)
 */
public class CustomerPaymentTransactionDto {

    private Long id;
    private BigDecimal amount;
    private String paymentDate;
    private String receiptNumber;
    private String status;

    public CustomerPaymentTransactionDto(PaymentTransaction transaction) {
        this.id = transaction.getId();
        this.amount = transaction.getAmount();
        this.paymentDate = transaction.getPaymentDate() != null ? transaction.getPaymentDate().toString() : null;
        this.receiptNumber = transaction.getReceiptNumber();
        this.status = transaction.getStatus();

        // Explicitly exclude:
        // - paymentMethod (internal tracking)
        // - referenceNumber (internal)
        // - notes (may contain internal info)
        // - recordedById (internal employee ID)
        // - tdsPercentage, tdsAmount, netAmount (company tax strategy)
        // - tdsDeductedBy (internal)
        // - paymentCategory (internal categorization)
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
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
}
