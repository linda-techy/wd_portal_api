package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.Addon;
import com.wd.api.estimation.service.calc.view.AddOnApplied;

public final class AddonMapper {
    private AddonMapper() {}

    public static AddOnApplied toView(Addon e) {
        return new AddOnApplied(e.getId(), e.getName(), e.getLumpAmount());
    }
}
