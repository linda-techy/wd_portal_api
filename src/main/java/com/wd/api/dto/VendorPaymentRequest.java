package com.wd.api.dto;

import com.wd.api.model.PortalUser;
import com.wd.api.model.PurchaseInvoice;
import com.wd.api.model.VendorPayment;
import com.wd.api.model.VendorPayment.PaymentMode;
import java.math.BigDecimal;
import java.time.LocalDate;

public record VendorPaymentRequest(
        PurchaseInvoice invoice, LocalDate paymentDate, BigDecimal amountPaid, BigDecimal tdsDeducted,
        BigDecimal otherDeductions, BigDecimal netPaid, PaymentMode paymentMode,
        String transactionReference, String chequeNumber, String bankName, PortalUser paidBy,
        PortalUser approvedBy, String notes) {
    public VendorPayment toEntity() {
        VendorPayment p = new VendorPayment();
        p.setInvoice(invoice); p.setPaymentDate(paymentDate); p.setAmountPaid(amountPaid);
        p.setTdsDeducted(tdsDeducted); p.setOtherDeductions(otherDeductions); p.setNetPaid(netPaid);
        p.setPaymentMode(paymentMode); p.setTransactionReference(transactionReference);
        p.setChequeNumber(chequeNumber); p.setBankName(bankName); p.setPaidBy(paidBy);
        p.setApprovedBy(approvedBy); p.setNotes(notes);
        return p;
    }
}
