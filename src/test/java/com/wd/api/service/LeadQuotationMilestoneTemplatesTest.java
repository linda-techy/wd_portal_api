package com.wd.api.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the per-projectType payment-schedule templates on
 * {@link LeadQuotationService}. Replaces the old hardcoded 30/40/25/5
 * waterfall on the customer-facing PDF with a sensible per-type schedule.
 *
 * <p>Two invariants matter for trust on the issued PDF:
 * <ol>
 *   <li>Percentages of every template must sum to exactly 1.00 — otherwise
 *       the milestone rupee amounts won't reconcile to the Final Amount the
 *       customer signed off on.</li>
 *   <li>Unknown / null project types fall back to the {@code DEFAULT}
 *       schedule rather than crashing or returning an empty list.</li>
 * </ol>
 */
class LeadQuotationMilestoneTemplatesTest {

    private static final BigDecimal SAMPLE_TOTAL = new BigDecimal("1000000"); // 10 lakh

    @Test
    void everyTemplate_sumsToExactlyOneHundredPercent() {
        for (String type : List.of(
                "RESIDENTIAL", "VILLA", "COMMERCIAL", "INTERIOR",
                "TURNKEY", "DEFAULT")) {
            List<Map<String, String>> rows = LeadQuotationService
                    .resolveMilestonesForProjectType(type, SAMPLE_TOTAL);

            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, String> r : rows) {
                // Each row's amountDisplay is INR-formatted; recompute from
                // the percentage label for the invariant check.
                String pct = r.get("pct").replace("%", "");
                sum = sum.add(new BigDecimal(pct));
            }
            assertThat(sum)
                    .as("milestones for %s must sum to 100%%", type)
                    .isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    @Test
    void unknownProjectType_fallsBackToDefaultSchedule() {
        List<Map<String, String>> mystery = LeadQuotationService
                .resolveMilestonesForProjectType("MOON_BASE", SAMPLE_TOTAL);
        List<Map<String, String>> fallback = LeadQuotationService
                .resolveMilestonesForProjectType("DEFAULT", SAMPLE_TOTAL);
        assertThat(mystery).usingRecursiveComparison().isEqualTo(fallback);
    }

    @Test
    void nullProjectType_fallsBackToDefaultSchedule() {
        List<Map<String, String>> nullType = LeadQuotationService
                .resolveMilestonesForProjectType(null, SAMPLE_TOTAL);
        List<Map<String, String>> fallback = LeadQuotationService
                .resolveMilestonesForProjectType("DEFAULT", SAMPLE_TOTAL);
        assertThat(nullType).usingRecursiveComparison().isEqualTo(fallback);
    }

    @Test
    void caseInsensitiveLookup() {
        List<Map<String, String>> mixedCase = LeadQuotationService
                .resolveMilestonesForProjectType("ResIDeNTial", SAMPLE_TOTAL);
        List<Map<String, String>> upper = LeadQuotationService
                .resolveMilestonesForProjectType("RESIDENTIAL", SAMPLE_TOTAL);
        assertThat(mixedCase).usingRecursiveComparison().isEqualTo(upper);
    }

    @Test
    void residentialSchedule_softens20PercentAdvance() {
        // Audit's primary complaint: hardcoded 30% advance is high for
        // Kerala small-residential. Residential template now leads at 20%.
        List<Map<String, String>> rows = LeadQuotationService
                .resolveMilestonesForProjectType("RESIDENTIAL", SAMPLE_TOTAL);
        assertThat(rows.get(0).get("pct")).isEqualTo("20%");
    }

    @Test
    void interiorSchedule_isFrontLoadedToProcurement() {
        // Interior fitouts spend most before site work — schedule reflects
        // the cashflow reality (40/40/20) instead of a generic waterfall.
        List<Map<String, String>> rows = LeadQuotationService
                .resolveMilestonesForProjectType("INTERIOR", SAMPLE_TOTAL);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("pct")).isEqualTo("40%");
    }

    @Test
    void rupeeAmounts_reconcileExactlyToFinalAmount() {
        // ₹10,00,000 final → milestone amounts must sum to ₹10,00,000
        // (HALF_UP rounding to whole rupees per row).
        List<Map<String, String>> rows = LeadQuotationService
                .resolveMilestonesForProjectType("VILLA", SAMPLE_TOTAL);
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, String> r : rows) {
            // amountDisplay is formatted "X,XX,XXX"; strip commas first.
            String raw = r.get("amountDisplay").replace(",", "");
            sum = sum.add(new BigDecimal(raw));
        }
        assertThat(sum).isEqualByComparingTo(SAMPLE_TOTAL);
    }
}
