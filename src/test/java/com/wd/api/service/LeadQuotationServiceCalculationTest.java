package com.wd.api.service;

import com.wd.api.model.Lead;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

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
@ExtendWith(MockitoExtension.class)
class LeadQuotationServiceCalculationTest {

    @Mock private LeadRepository leadRepository;

    private final LeadQuotationService service = new LeadQuotationService();

    @BeforeEach
    void wireMocks() {
        // LINE_ITEM tests don't touch the lead repo, but the service holds
        // it as an @Autowired field; inject the mock so SQFT_RATE tests can
        // stub it. Reflection (vs constructor injection) keeps the test
        // independent of any future DI refactor. @BeforeEach runs *after*
        // Mockito populates @Mock fields, which an instance initializer
        // wouldn't.
        ReflectionTestUtils.setField(service, "leadRepository", leadRepository);
    }

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

    @Test
    void newLeadQuotation_defaultsToLineItemPricingMode() {
        // Entity default is LINE_ITEM for back-compat — the Flutter form
        // upgrades the mode to SQFT_RATE for new customer-facing quotations.
        LeadQuotation q = new LeadQuotation();
        assertThat(q.getPricingMode()).isEqualTo("LINE_ITEM");
    }

    // ── SQFT_RATE pricing mode ────────────────────────────────────────────

    @Test
    void calculateTotals_sqftRate_subtotalIsAreaTimesRate() {
        // Mr Clinton's actual quotation — 2,050 ₹/sqft.
        Lead lead = new Lead();
        lead.setProjectSqftArea(new BigDecimal("1800"));
        lenient().when(leadRepository.findById(7L)).thenReturn(Optional.of(lead));

        LeadQuotation q = new LeadQuotation();
        q.setPricingMode("SQFT_RATE");
        q.setLeadId(7L);
        q.setRatePerSqft(new BigDecimal("2050.00"));
        q.setTaxRatePercent(new BigDecimal("18.00"));

        service.calculateTotals(q);

        // 1800 × 2050 = 36,90,000 subtotal
        assertThat(q.getTotalAmount()).isEqualByComparingTo("3690000");
        // tax @ 18% on 36,90,000 = 6,64,200
        assertThat(q.getTaxAmount()).isEqualByComparingTo("664200.00");
        // final = 43,54,200
        assertThat(q.getFinalAmount()).isEqualByComparingTo("4354200.00");
    }

    @Test
    void calculateTotals_sqftRate_ignoresLineItems() {
        // Even if the items list has priced rows (e.g. from a duplicated
        // LINE_ITEM source), SQFT_RATE math wins — items become scope specs.
        Lead lead = new Lead();
        lead.setProjectSqftArea(new BigDecimal("1000"));
        lenient().when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));

        LeadQuotation q = new LeadQuotation();
        q.setPricingMode("SQFT_RATE");
        q.setLeadId(1L);
        q.setRatePerSqft(new BigDecimal("2000"));
        q.addItem(item("999999.00")); // would dominate under LINE_ITEM
        q.setTaxRatePercent(null);    // disable auto-tax for clarity

        service.calculateTotals(q);

        assertThat(q.getTotalAmount()).isEqualByComparingTo("2000000"); // 1000 × 2000
        assertThat(q.getFinalAmount()).isEqualByComparingTo("2000000");
    }

    @Test
    void calculateTotals_sqftRate_collapsesToZeroWhenSqftMissing() {
        // Lead has no projectSqftArea on file → subtotal is 0 (not a crash).
        // Lets staff save a draft before knowing the area.
        Lead lead = new Lead();
        lead.setProjectSqftArea(null);
        lenient().when(leadRepository.findById(2L)).thenReturn(Optional.of(lead));

        LeadQuotation q = new LeadQuotation();
        q.setPricingMode("SQFT_RATE");
        q.setLeadId(2L);
        q.setRatePerSqft(new BigDecimal("2050"));
        q.setTaxRatePercent(null);

        service.calculateTotals(q);
        assertThat(q.getTotalAmount()).isEqualByComparingTo("0");
        assertThat(q.getFinalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void calculateTotals_sqftRate_collapsesToZeroWhenRateMissing() {
        // Symmetric: rate not yet entered → subtotal 0.
        Lead lead = new Lead();
        lead.setProjectSqftArea(new BigDecimal("1800"));
        lenient().when(leadRepository.findById(3L)).thenReturn(Optional.of(lead));

        LeadQuotation q = new LeadQuotation();
        q.setPricingMode("SQFT_RATE");
        q.setLeadId(3L);
        q.setRatePerSqft(null);
        q.setTaxRatePercent(null);

        service.calculateTotals(q);
        assertThat(q.getTotalAmount()).isEqualByComparingTo("0");
    }

    private static LeadQuotationItem item(String totalPrice) {
        LeadQuotationItem i = new LeadQuotationItem();
        i.setQuantity(BigDecimal.ONE);
        i.setUnitPrice(new BigDecimal(totalPrice));
        i.setTotalPrice(new BigDecimal(totalPrice));
        return i;
    }
}
