package com.wd.api.dto.quotation;

import java.math.BigDecimal;

/**
 * Summary statistics for the lead-quotation pipeline, fed to the Flutter
 * list-screen hero card so staff see "what's open, what closed, how are we
 * doing" at a glance instead of having to scan the table.
 *
 * <p>{@code openValue} aggregates {@code DRAFT}, {@code SENT}, {@code VIEWED}
 * — anything still actionable. {@code acceptedValue} and the win-rate /
 * close-time stats are computed over a 90-day window so a single quarter's
 * pipeline drives the dashboard rather than the full historical archive.
 *
 * @param openCount       number of open quotations (DRAFT / SENT / VIEWED)
 * @param openValue       sum of {@code finalAmount} over open quotations
 * @param acceptedCount   number of quotations accepted in the last 90 days
 * @param acceptedValue   sum of {@code finalAmount} accepted in the last 90 days
 * @param winRatePercent  acceptedCount / (acceptedCount + rejectedCount) × 100,
 *                        evaluated over the same 90-day window. {@code 0} when
 *                        no quotations have closed yet.
 * @param avgCloseDays    average days from {@code sentAt} to {@code respondedAt}
 *                        for accepted quotations in the window. {@code null}
 *                        when no closes are available yet.
 */
public record PipelineSummaryResponse(
        long openCount,
        BigDecimal openValue,
        long acceptedCount,
        BigDecimal acceptedValue,
        double winRatePercent,
        Double avgCloseDays
) {}
