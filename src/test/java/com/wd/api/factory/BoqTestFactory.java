package com.wd.api.factory;

import com.wd.api.model.BoqCategory;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.BoqItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.BoqItemStatus;
import com.wd.api.model.enums.ItemKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Static factory methods for creating BOQ-related test fixtures.
 * <p>
 * Returned entities are NOT persisted -- the calling test is responsible for
 * saving them via repositories after wiring up required relationships.
 */
public final class BoqTestFactory {

    private BoqTestFactory() {}

    /**
     * Creates a {@link BoqDocument} in DRAFT status for the given project.
     */
    public static BoqDocument createDocument(CustomerProject project) {
        BoqDocument doc = new BoqDocument();
        doc.setProject(project);
        doc.setStatus(BoqDocumentStatus.DRAFT);
        doc.setRevisionNumber(1);
        doc.setTotalValueExGst(BigDecimal.ZERO);
        doc.setGstRate(new BigDecimal("0.18"));
        doc.setTotalGstAmount(BigDecimal.ZERO);
        doc.setTotalValueInclGst(BigDecimal.ZERO);
        return doc;
    }

    /**
     * Creates a {@link BoqItem} linked to the given project and document.
     *
     * @param project     the owning project
     * @param doc         the parent BOQ document (may be null for standalone items)
     * @param description line-item description
     * @param qty         planned quantity
     * @param rate        unit rate
     */
    public static BoqItem createItem(CustomerProject project, BoqDocument doc,
                                     String description, BigDecimal qty, BigDecimal rate) {
        BoqItem item = new BoqItem();
        item.setProject(project);
        item.setBoqDocument(doc);
        item.setDescription(description);
        item.setUnit("nos");
        item.setQuantity(qty);
        item.setUnitRate(rate);
        item.setTotalAmount(qty.multiply(rate).setScale(6, RoundingMode.HALF_UP));
        item.setStatus(BoqItemStatus.DRAFT);
        item.setItemKind(ItemKind.BASE);
        return item;
    }

    /**
     * Creates a {@link BoqCategory} for the given project.
     */
    public static BoqCategory createCategory(CustomerProject project, String name) {
        BoqCategory cat = new BoqCategory();
        cat.setProject(project);
        cat.setName(name);
        cat.setDescription("Test category: " + name);
        return cat;
    }

    /**
     * Convenience method that creates a DRAFT {@link BoqDocument} with {@code count}
     * items and a single category. Useful for tests that need a populated BOQ without
     * caring about specific line-item details.
     * <p>
     * The returned record bundles the document, category, and items together.
     * None of the entities are persisted.
     *
     * @param project the owning project
     * @param count   number of BOQ items to generate
     * @return a {@link BoqBundle} containing the document, category, and items
     */
    public static BoqBundle withItems(CustomerProject project, int count) {
        BoqDocument doc = createDocument(project);
        BoqCategory category = createCategory(project, "General Works");

        List<BoqItem> items = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (int i = 1; i <= count; i++) {
            BigDecimal qty = new BigDecimal(10 * i);
            BigDecimal rate = new BigDecimal("100.00");
            BoqItem item = createItem(project, doc, "Test BOQ Item " + i, qty, rate);
            item.setCategory(category);
            items.add(item);
            runningTotal = runningTotal.add(item.getTotalAmount());
        }

        // Update document totals
        doc.setTotalValueExGst(runningTotal);
        BigDecimal gst = runningTotal.multiply(doc.getGstRate()).setScale(6, RoundingMode.HALF_UP);
        doc.setTotalGstAmount(gst);
        doc.setTotalValueInclGst(runningTotal.add(gst));

        return new BoqBundle(doc, category, items);
    }

    /**
     * A simple record bundling the entities created by {@link #withItems}.
     */
    public record BoqBundle(BoqDocument document, BoqCategory category, List<BoqItem> items) {}
}
