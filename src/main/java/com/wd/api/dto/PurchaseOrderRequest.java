package com.wd.api.dto;

import com.wd.api.model.MaterialIndent;
import com.wd.api.model.PurchaseOrder;
import com.wd.api.model.PurchaseOrderItem;
import com.wd.api.model.VendorQuotation;
import com.wd.api.model.enums.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        String poNumber,
        MaterialIndent indent,
        VendorQuotation quotation,
        LocalDate poDate,
        LocalDate expectedDeliveryDate,
        BigDecimal totalAmount,
        BigDecimal gstAmount,
        BigDecimal netAmount,
        PurchaseOrderStatus status,
        String notes,
        List<PurchaseOrderItem> items) {

    public PurchaseOrder toEntity() {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setIndent(indent);
        po.setQuotation(quotation);
        po.setPoDate(poDate);
        po.setExpectedDeliveryDate(expectedDeliveryDate);
        po.setTotalAmount(totalAmount);
        po.setGstAmount(gstAmount);
        po.setNetAmount(netAmount);
        po.setStatus(status);
        po.setNotes(notes);
        po.setItems(items);
        return po;
    }
}
