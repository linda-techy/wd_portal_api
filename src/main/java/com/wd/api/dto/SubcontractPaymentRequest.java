package com.wd.api.dto;

import com.wd.api.model.SubcontractPayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubcontractPaymentRequest(
        LocalDate paymentDate,
        BigDecimal grossAmount,
        BigDecimal tdsPercentage,
        String paymentMode
) {
    public SubcontractPayment toEntity() {
        SubcontractPayment p = new SubcontractPayment();
        p.setPaymentDate(paymentDate);
        p.setGrossAmount(grossAmount);
        p.setTdsPercentage(tdsPercentage);
        p.setPaymentMode(paymentMode);
        return p;
    }
}
