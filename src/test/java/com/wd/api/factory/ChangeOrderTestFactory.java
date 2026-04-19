package com.wd.api.factory;

import com.wd.api.model.ChangeOrder;
import com.wd.api.model.ChangeOrderLineItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ChangeOrderStatus;
import com.wd.api.model.enums.ChangeOrderType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Static factory methods for creating {@link ChangeOrder} test fixtures.
 * <p>
 * Returned entities are NOT persisted. The caller must set the required
 * {@code boqDocument} relationship and save via repositories.
 */
public final class ChangeOrderTestFactory {

    private ChangeOrderTestFactory() {}

    /**
     * Creates a DRAFT {@link ChangeOrder} for the given project.
     * <p>
     * <strong>Note:</strong> The caller must set {@code boqDocument} before persisting,
     * as that field is non-nullable in the schema.
     */
    public static ChangeOrder create(CustomerProject project) {
        ChangeOrder co = new ChangeOrder();
        co.setProject(project);
        co.setReferenceNumber("CO-TEST-001");
        co.setTitle("Test Change Order");
        co.setDescription("Test change order description");
        co.setJustification("Test justification");
        co.setCoType(ChangeOrderType.SCOPE_ADDITION);
        co.setStatus(ChangeOrderStatus.DRAFT);
        co.setNetAmountExGst(BigDecimal.ZERO);
        co.setGstRate(new BigDecimal("0.18"));
        co.setGstAmount(BigDecimal.ZERO);
        co.setNetAmountInclGst(BigDecimal.ZERO);
        return co;
    }

    /**
     * Creates a DRAFT {@link ChangeOrder} with {@code count} line items.
     * Financial totals are calculated from the generated line items.
     * <p>
     * <strong>Note:</strong> The caller must set {@code boqDocument} before persisting.
     *
     * @param project the owning project
     * @param count   number of line items to generate
     * @return a {@link ChangeOrderBundle} containing the change order and its line items
     */
    public static ChangeOrderBundle withLineItems(CustomerProject project, int count) {
        ChangeOrder co = create(project);

        List<ChangeOrderLineItem> lineItems = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (int i = 1; i <= count; i++) {
            ChangeOrderLineItem li = new ChangeOrderLineItem();
            li.setChangeOrder(co);
            li.setDescription("CO Line Item " + i);
            li.setUnit("nos");

            BigDecimal qty = new BigDecimal(5 * i);
            BigDecimal rate = new BigDecimal("200.00");
            BigDecimal lineAmount = qty.multiply(rate).setScale(6, RoundingMode.HALF_UP);

            li.setNewQuantity(qty);
            li.setDeltaQuantity(qty);
            li.setUnitRate(rate);
            li.setNewRate(rate);
            li.setLineAmountExGst(lineAmount);

            lineItems.add(li);
            runningTotal = runningTotal.add(lineAmount);
        }

        co.setLineItems(lineItems);
        co.setNetAmountExGst(runningTotal);
        BigDecimal gst = runningTotal.multiply(co.getGstRate()).setScale(6, RoundingMode.HALF_UP);
        co.setGstAmount(gst);
        co.setNetAmountInclGst(runningTotal.add(gst));

        return new ChangeOrderBundle(co, lineItems);
    }

    /**
     * A simple record bundling a change order with its line items.
     */
    public record ChangeOrderBundle(ChangeOrder changeOrder, List<ChangeOrderLineItem> lineItems) {}
}
