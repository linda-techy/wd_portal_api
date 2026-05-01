package com.wd.api.estimation.dto.admin;

import com.wd.api.estimation.domain.MarketIndexSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record MarketIndexResponse(
        UUID id,
        LocalDate snapshotDate,
        BigDecimal steelRate,
        BigDecimal cementRate,
        BigDecimal sandRate,
        BigDecimal aggregateRate,
        BigDecimal tilesRate,
        BigDecimal electricalRate,
        BigDecimal paintsRate,
        Map<String, Object> weights,
        BigDecimal compositeIndex,
        boolean active) {

    public static MarketIndexResponse fromEntity(MarketIndexSnapshot s) {
        return new MarketIndexResponse(
                s.getId(), s.getSnapshotDate(),
                s.getSteelRate(), s.getCementRate(), s.getSandRate(),
                s.getAggregateRate(), s.getTilesRate(),
                s.getElectricalRate(), s.getPaintsRate(),
                s.getWeightsJson(), s.getCompositeIndex(), s.isActive());
    }
}
