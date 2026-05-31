package com.wd.api.dto;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectInvoice;
import com.wd.api.model.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceiptRequest(
        CustomerProject project, ProjectInvoice invoice, String receiptNumber, BigDecimal amount,
        LocalDate paymentDate, String paymentMethod, String transactionReference, String notes) {
    public Receipt toEntity() {
        Receipt r = new Receipt();
        r.setProject(project); r.setInvoice(invoice); r.setReceiptNumber(receiptNumber);
        r.setAmount(amount); r.setPaymentDate(paymentDate); r.setPaymentMethod(paymentMethod);
        r.setTransactionReference(transactionReference); r.setNotes(notes);
        return r;
    }
}
