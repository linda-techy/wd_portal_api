package com.wd.api.estimation.domain.enums;

import java.math.BigDecimal;

/**
 * P — Sales-set confidence for a budgetary estimation, controlling the ±band
 * the calculator applies around (area × baseRate).
 *
 * LOW (±10%): pre-site-visit, minimal info
 * MEDIUM (±5%): site visit done, requirements clear
 * HIGH (±3%): architect plan ~80% locked
 *
 * Stored as a VARCHAR; null on line-item rows.
 */
public enum EstimationConfidenceLevel {
    LOW(new BigDecimal("0.10")),
    MEDIUM(new BigDecimal("0.05")),
    HIGH(new BigDecimal("0.03"));

    public final BigDecimal bandPercent;

    EstimationConfidenceLevel(BigDecimal bandPercent) {
        this.bandPercent = bandPercent;
    }
}
