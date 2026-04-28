package com.wd.api.service.dpc;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the static formatters in {@link DpcRenderService}.
 *
 * <p>The DPC PDF must render "—" for sqft-derived cells whenever the project
 * has no built-up area on file (regression: project 47's REV 01 showed
 * "0 sqft" and "INR 0 / sqft" everywhere instead of "—").
 */
class DpcRenderServiceTest {

    @Test
    void formatINR_zero_returnsZeroString() {
        assertThat(DpcRenderService.formatINR(BigDecimal.ZERO)).isEqualTo("0");
    }

    @Test
    void formatINR_indianGroupingForLakhs() {
        assertThat(DpcRenderService.formatINR(new BigDecimal("4728200"))).isEqualTo("47,28,200");
    }

    @Test
    void formatINROrDash_nullRendersAsDash() {
        assertThat(DpcRenderService.formatINROrDash(null)).isEqualTo("—");
    }

    @Test
    void formatINROrDash_zeroStillRendersAsZero() {
        // Zero rupees is a real value (e.g. an empty scope) — only the
        // "no value computable" case (null) collapses to a dash.
        assertThat(DpcRenderService.formatINROrDash(BigDecimal.ZERO)).isEqualTo("0");
    }

    @Test
    void formatINROrDash_positiveFormatsWithIndianGrouping() {
        assertThat(DpcRenderService.formatINROrDash(new BigDecimal("100000"))).isEqualTo("1,00,000");
    }

    @Test
    void formatSqftOrDash_nullRendersAsDash() {
        assertThat(DpcRenderService.formatSqftOrDash(null)).isEqualTo("—");
    }

    @Test
    void formatSqftOrDash_zeroRendersAsDash() {
        // For sqft, zero means "the project has no built-up area on file",
        // which is the same UX state as null — render "—" so customers don't
        // see "0 sqft" on a customer-facing artifact.
        assertThat(DpcRenderService.formatSqftOrDash(BigDecimal.ZERO)).isEqualTo("—");
    }

    @Test
    void formatSqftOrDash_positiveFormatsAsInteger() {
        assertThat(DpcRenderService.formatSqftOrDash(new BigDecimal("1800"))).isEqualTo("1,800");
        assertThat(DpcRenderService.formatSqftOrDash(new BigDecimal("1800.45")))
                .isEqualTo("1,800");
    }
}
