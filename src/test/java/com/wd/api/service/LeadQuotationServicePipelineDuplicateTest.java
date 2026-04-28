package com.wd.api.service;

import com.wd.api.dto.quotation.PipelineSummaryResponse;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.repository.LeadQuotationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the two pipeline-hero / duplicate methods on
 * {@link LeadQuotationService}. The arithmetic + categorisation rules in
 * {@code getPipelineSummary} and the copy semantics in
 * {@code duplicateQuotation} are pure functions of the inputs — pure-Mockito
 * tests are the right shape, no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class LeadQuotationServicePipelineDuplicateTest {

    @Mock private LeadQuotationRepository quotationRepository;

    @InjectMocks
    private LeadQuotationService service;

    // ── Pipeline summary ──────────────────────────────────────────────────

    @Test
    void getPipelineSummary_aggregatesOpenAndClosedBuckets() {
        LocalDateTime now = LocalDateTime.now();
        // Mix of open + closed rows. Field order matches the JPQL projection
        // in pipelineRowsSince: [status, finalAmount, sentAt, respondedAt].
        List<Object[]> rows = List.of(
                row("DRAFT",    "100000", null,            null),
                row("SENT",     "200000", now.minusDays(5), null),
                row("VIEWED",   "300000", now.minusDays(3), null),
                row("ACCEPTED", "500000", now.minusDays(20), now.minusDays(15)),
                row("ACCEPTED", "750000", now.minusDays(40), now.minusDays(35)),
                row("REJECTED", "120000", now.minusDays(10), now.minusDays(8))
        );
        when(quotationRepository.pipelineRowsSince(any())).thenReturn(rows);

        PipelineSummaryResponse summary = service.getPipelineSummary();

        // Open: DRAFT + SENT + VIEWED → 3 rows, ₹6,00,000
        assertThat(summary.openCount()).isEqualTo(3L);
        assertThat(summary.openValue()).isEqualByComparingTo(new BigDecimal("600000"));

        // Accepted: 2 rows, ₹12,50,000
        assertThat(summary.acceptedCount()).isEqualTo(2L);
        assertThat(summary.acceptedValue()).isEqualByComparingTo(new BigDecimal("1250000"));

        // Win rate = 2 / (2 + 1) = 66.6%
        assertThat(summary.winRatePercent()).isCloseTo(66.66, within(0.1));

        // Avg close days = ((20-15) + (40-35)) / 2 = 5.0
        assertThat(summary.avgCloseDays()).isCloseTo(5.0, within(0.01));
    }

    @Test
    void getPipelineSummary_handlesEmptyPipeline() {
        when(quotationRepository.pipelineRowsSince(any())).thenReturn(List.of());

        PipelineSummaryResponse summary = service.getPipelineSummary();

        assertThat(summary.openCount()).isZero();
        assertThat(summary.openValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.acceptedCount()).isZero();
        assertThat(summary.acceptedValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.winRatePercent()).isZero();
        // No closes yet → avgCloseDays is null (renderer shows "—").
        assertThat(summary.avgCloseDays()).isNull();
    }

    @Test
    void getPipelineSummary_winRateZeroWhenNothingClosed() {
        // Only open quotations, nothing accepted or rejected.
        List<Object[]> rows = List.of(
                row("DRAFT", "100000", null, null),
                row("SENT",  "200000", LocalDateTime.now().minusDays(2), null)
        );
        when(quotationRepository.pipelineRowsSince(any())).thenReturn(rows);

        PipelineSummaryResponse summary = service.getPipelineSummary();

        assertThat(summary.openCount()).isEqualTo(2L);
        assertThat(summary.winRatePercent()).isZero();
        assertThat(summary.avgCloseDays()).isNull();
    }

    // ── Duplicate quotation ───────────────────────────────────────────────

    @Test
    void duplicateQuotation_copiesHeaderItemsPricingResetsLifecycle() {
        // Source: an ACCEPTED quotation with two items and a discount + rate.
        LeadQuotation src = new LeadQuotation();
        src.setId(99L);
        src.setLeadId(7L);
        src.setQuotationNumber("QUO-20260101-0042");
        src.setTitle("Villa for Mr. Joseph");
        src.setDescription("Original villa scope");
        src.setValidityDays(45);
        src.setTaxRatePercent(new BigDecimal("12.00"));
        src.setDiscountAmount(new BigDecimal("5000.00"));
        src.setNotes("Original notes");
        src.setStatus("ACCEPTED");
        src.setSentAt(LocalDateTime.now().minusDays(20));
        src.setRespondedAt(LocalDateTime.now().minusDays(10));
        src.addItem(item(1, "Foundation", "100000.00"));
        src.addItem(item(2, "Slab",       "200000.00"));

        when(quotationRepository.findByIdWithItems(99L)).thenReturn(Optional.of(src));
        when(quotationRepository.nextQuotationSequenceValue()).thenReturn(99L);
        when(quotationRepository.save(any(LeadQuotation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LeadQuotation copy = service.duplicateQuotation(99L, 5L);

        // Header carried over with " (Copy)" suffix.
        assertThat(copy.getTitle()).isEqualTo("Villa for Mr. Joseph (Copy)");
        assertThat(copy.getLeadId()).isEqualTo(7L);
        assertThat(copy.getDescription()).isEqualTo("Original villa scope");
        assertThat(copy.getValidityDays()).isEqualTo(45);
        assertThat(copy.getNotes()).isEqualTo("Original notes");

        // Pricing knobs preserved.
        assertThat(copy.getTaxRatePercent()).isEqualByComparingTo("12.00");
        assertThat(copy.getDiscountAmount()).isEqualByComparingTo("5000.00");

        // Lifecycle reset: fresh DRAFT, no sent/responded timestamps,
        // identifiers regenerated, currentUser is the creator.
        assertThat(copy.getStatus()).isEqualTo("DRAFT");
        assertThat(copy.getSentAt()).isNull();
        assertThat(copy.getRespondedAt()).isNull();
        assertThat(copy.getId()).isNull();
        // Walldot reference format: YYYY/MM/DD/A<seq> (matches the
        // company's actual paper quotations, e.g. "2026/02/04/A6").
        assertThat(copy.getQuotationNumber()).matches("\\d{4}/\\d{2}/\\d{2}/A99");
        assertThat(copy.getCreatedById()).isEqualTo(5L);

        // Items: same content, new instances (no FK collision with source).
        assertThat(copy.getItems()).hasSize(2);
        assertThat(copy.getItems().get(0).getDescription()).isEqualTo("Foundation");
        assertThat(copy.getItems().get(1).getDescription()).isEqualTo("Slab");
        for (LeadQuotationItem i : copy.getItems()) {
            assertThat(i.getId()).isNull(); // fresh persistence
            assertThat(i.getQuotation()).isSameAs(copy);
        }

        // Totals recomputed under the canonical calc — backend authority.
        // subtotal 3,00,000 - discount 5,000 = 2,95,000; tax @12% = 35,400.
        assertThat(copy.getTotalAmount()).isEqualByComparingTo("300000");
        assertThat(copy.getTaxAmount()).isEqualByComparingTo("35400.00");
        assertThat(copy.getFinalAmount()).isEqualByComparingTo("330400.00");
    }

    @Test
    void duplicateQuotation_throwsWhenSourceMissing() {
        when(quotationRepository.findByIdWithItems(anyLong())).thenReturn(Optional.empty());

        try {
            service.duplicateQuotation(404L, 1L);
            org.junit.jupiter.api.Assertions.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }

    // ── Restore (soft-delete undo) ────────────────────────────────────────

    @Test
    void restoreQuotation_returnsRestoredRowWhenTombstoneCleared() {
        LeadQuotation restored = new LeadQuotation();
        restored.setId(42L);
        restored.setStatus("DRAFT");
        restored.setTitle("Resurrected");

        when(quotationRepository.restoreById(42L)).thenReturn(1);
        when(quotationRepository.findById(42L)).thenReturn(Optional.of(restored));

        LeadQuotation result = service.restoreQuotation(42L);

        assertThat(result).isSameAs(restored);
        assertThat(result.getTitle()).isEqualTo("Resurrected");
    }

    @Test
    void restoreQuotation_throwsWhenNothingToRestore() {
        // Row not deleted (or doesn't exist) → restoreById returns 0.
        when(quotationRepository.restoreById(anyLong())).thenReturn(0);

        try {
            service.restoreQuotation(404L);
            org.junit.jupiter.api.Assertions.fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found or not deleted");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Build a row tuple matching the JPQL projection in pipelineRowsSince. */
    private static Object[] row(String status, String finalAmount,
                                LocalDateTime sentAt, LocalDateTime respondedAt) {
        return new Object[]{
                status,
                new BigDecimal(finalAmount),
                sentAt,
                respondedAt
        };
    }

    private static LeadQuotationItem item(int number, String description, String total) {
        LeadQuotationItem i = new LeadQuotationItem();
        i.setItemNumber(number);
        i.setDescription(description);
        i.setQuantity(BigDecimal.ONE);
        i.setUnitPrice(new BigDecimal(total));
        i.setTotalPrice(new BigDecimal(total));
        return i;
    }
}
