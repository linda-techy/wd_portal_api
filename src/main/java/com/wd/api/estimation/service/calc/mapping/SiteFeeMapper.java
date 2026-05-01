package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.SiteFee;
import com.wd.api.estimation.service.calc.view.SiteFeeApplied;

public final class SiteFeeMapper {
    private SiteFeeMapper() {}

    public static SiteFeeApplied toView(SiteFee e) {
        return new SiteFeeApplied(
                e.getId(), e.getName(), e.getMode(),
                e.getLumpAmount(), e.getPerSqftRate());
    }
}
