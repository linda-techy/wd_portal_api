package com.wd.api.dto.dpc;

import java.math.BigDecimal;
import java.util.List;

/**
 * Master cost summary for a DPC document.
 *
 * Aggregates the per-scope rollups into project-level totals and per-sqft
 * figures.  Holds no persisted state — recomputed on every read.
 */
public record DpcMasterCostSummaryDto(
        BigDecimal totalOriginal,
        BigDecimal totalCustomized,
        BigDecimal totalVariance,
        BigDecimal originalPerSqft,
        BigDecimal customizedPerSqft,
        BigDecimal sqfeet,
        List<DpcCostRollupDto> scopes
) {
}
