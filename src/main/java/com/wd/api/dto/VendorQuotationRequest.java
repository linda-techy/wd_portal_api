package com.wd.api.dto;

import com.wd.api.model.VendorQuotation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record VendorQuotationRequest(
        BigDecimal quotedAmount,
        String itemsIncluded,
        BigDecimal deliveryCharges,
        BigDecimal taxAmount,
        LocalDate expectedDeliveryDate,
        LocalDate validUntil,
        String documentUrl,
        String notes) {

    public VendorQuotation toEntity() {
        VendorQuotation q = new VendorQuotation();
        q.setQuotedAmount(quotedAmount);
        q.setItemsIncluded(itemsIncluded);
        q.setDeliveryCharges(deliveryCharges);
        q.setTaxAmount(taxAmount);
        q.setExpectedDeliveryDate(expectedDeliveryDate);
        q.setValidUntil(validUntil);
        q.setDocumentUrl(documentUrl);
        q.setNotes(notes);
        return q;
    }
}
