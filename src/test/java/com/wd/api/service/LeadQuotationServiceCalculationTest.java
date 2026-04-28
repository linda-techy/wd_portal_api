package com.wd.api.service;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LeadQuotationService#calculateTotals(LeadQuotation)}.
 *
 * <p>Calc rules (Indian GST convention):
 * <pre>
 *   subtotal       = SUM(item.totalPrice)
 *   discountedBase = subtotal − discount       (discount ≤ subtotal)
 *   tax            = discountedBase × rate / 100   (when rate is set)
 *   final          = discountedBase + tax
 * </pre>
 *
 * <p>Regression: previously the service used {@code subtotal + tax − discount}
 * with a manually-entered tax amount, which over- or under-charged GST any
 * time staff didn't manually compute the tax against the discounted base.
 */
class LeadQuotationServiceCalculationTest {

    private final LeadQuotationService service = new LeadQuotationService();

    @Test
    void calculateTotals_withTaxRatePercent_appliesRateToDiscountedBase() {
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("100000.00")); // subtotal 1,00,000
        q.setDiscountAmount(new BigDecimal("10000.00"));
        q.setTaxRatePercent(new BigDecimal("18.00"));

        service.calculateTotals(q);

        assertThat(q.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
        // tax = (100000 − 10000) × 18 / 100 = 16,200
        assertThat(q.getTaxAmount()).isEqualByComparingTo(new BigDecimal("16200.00"));
        // final = 90000 + 16200 = 1,06,200
        assertThat(q.getFinalAmount()).isEqualByComparingTo(new BigDecimal("106200.00"));
    }

    @Test
    void calculateTotals_withTaxRatePercent_overwritesManualTaxAmount() {
        // When a rate is set, the auto-computed tax replaces any
        // staff-entered amount — the rate is the source of truth.
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("50000.00"));
        q.setDiscountAmount(new BigDecimal("0.00"));
        q.setTaxAmount(new BigDecimal("999999.00")); // bogus manual entry
        q.setTaxRatePercent(new BigDecimal("18.00"));

        service.calculateTotals(q);

        assertThat(q.getTaxAmount()).isEqualByComparingTo(new BigDecimal("9000.00")); // 50000 × 18%
        assertThat(q.getFinalAmount()).isEqualByComparingTo(new BigDecimal("59000.00"));
    }

    @Test
    void calculateTotals_withRateNull_preservesManualTaxAmount() {
        // Backwards compatibility: legacy rows with rate=null behave as before.
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("100000.00"));
        q.setTaxRatePercent(null);
        q.setTaxAmount(new BigDecimal("18000.00"));
        q.setDiscountAmount(new BigDecimal("5000.00"));

        service.calculateTotals(q);

        assertThat(q.getTaxAmount()).isEqualByComparingTo(new BigDecimal("18000.00"));
        // final = (100000 − 5000) + 18000 = 1,13,000
        assertThat(q.getFinalAmount()).isEqualByComparingTo(new BigDecimal("113000.00"));
    }

    @Test
    void calculateTotals_zeroDiscount_isHandledCleanly() {
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("80000.00"));
        q.setTaxRatePercent(new BigDecimal("18.00"));
        // No discount set at all (null).

        service.calculateTotals(q);

        assertThat(q.getTaxAmount()).isEqualByComparingTo(new BigDecimal("14400.00"));
        assertThat(q.getFinalAmount()).isEqualByComparingTo(new BigDecimal("94400.00"));
    }

    @Test
    void calculateTotals_throwsWhenDiscountExceedsSubtotal() {
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("10000.00"));
        q.setDiscountAmount(new BigDecimal("15000.00")); // discount > subtotal

        assertThatThrownBy(() -> service.calculateTotals(q))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContainingAll("discount", "subtotal");
    }

    @Test
    void calculateTotals_throwsOnNegativeDiscount() {
        LeadQuotation q = new LeadQuotation();
        q.addItem(item("10000.00"));
        q.setDiscountAmount(new BigDecimal("-100.00"));

        assertThatThrownBy(() -> service.calculateTotals(q))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void newLeadQuotation_defaultsTaxRatePercentTo18() {
        // New entity instances default to 18% so staff don't have to remember
        // to set GST on every quote — the most common case is auto-correct.
        LeadQuotation q = new LeadQuotation();
        assertThat(q.getTaxRatePercent()).isEqualByComparingTo(new BigDecimal("18.00"));
    }

    private static LeadQuotationItem item(String totalPrice) {
        LeadQuotationItem i = new LeadQuotationItem();
        i.setQuantity(BigDecimal.ONE);
        i.setUnitPrice(new BigDecimal(totalPrice));
        i.setTotalPrice(new BigDecimal(totalPrice));
        return i;
    }
}
