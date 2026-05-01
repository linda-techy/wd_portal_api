package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import com.wd.api.estimation.service.calc.view.MarketIndexSnapshotView;

public final class MarketIndexMapper {
    private MarketIndexMapper() {}

    public static MarketIndexSnapshotView toView(MarketIndexSnapshot e) {
        return new MarketIndexSnapshotView(
                e.getId(), e.getSnapshotDate(), e.getCompositeIndex(), e.getWeightsJson());
    }
}
