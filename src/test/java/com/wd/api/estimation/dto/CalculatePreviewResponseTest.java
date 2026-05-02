package com.wd.api.estimation.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.estimation.domain.enums.LineType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatePreviewResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void response_canBeConstructedAndSerialised() throws Exception {
        LineItemDto base = new LineItemDto(
                LineType.BASE, "Base package cost (Signature, 1050 sqft)",
                UUID.randomUUID(), new BigDecimal("1050"), "sqft",
                new BigDecimal("2350"), new BigDecimal("2467500.00"), 1);
        CalculatePreviewResponse resp = new CalculatePreviewResponse(
                new BigDecimal("1050"),
                new BigDecimal("2467500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("2467500.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("2467500.00"),
                new BigDecimal("444150.00"),
                new BigDecimal("2911650.00"),
                List.of(base),
                List.of("market-index-stale-14-days"),
                null, null);

        String json = mapper.writeValueAsString(resp);
        assertThat(json).contains("\"chargeableArea\":1050");
        assertThat(json).contains("\"grandTotal\":2911650.00");
        assertThat(json).contains("\"warnings\":[\"market-index-stale-14-days\"]");
        assertThat(json).contains("\"lineItems\":[");
        // BigDecimal must serialise as JSON number, not string
        assertThat(json).doesNotContain("\"grandTotal\":\"2911650");
    }

    @Test
    void emptyWarnings_serialiseAsEmptyArray() throws Exception {
        CalculatePreviewResponse resp = new CalculatePreviewResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(),
                null, null);
        String json = mapper.writeValueAsString(resp);
        assertThat(json).contains("\"warnings\":[]");
        assertThat(json).contains("\"lineItems\":[]");
    }
}
