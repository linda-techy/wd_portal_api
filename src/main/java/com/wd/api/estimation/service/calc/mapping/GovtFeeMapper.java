package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.GovtFee;
import com.wd.api.estimation.service.calc.view.GovtFeeApplied;

public final class GovtFeeMapper {
    private GovtFeeMapper() {}

    public static GovtFeeApplied toView(GovtFee e) {
        return new GovtFeeApplied(e.getId(), e.getName(), e.getLumpAmount());
    }
}
