package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.service.calc.view.PackageRateVersionView;

public final class RateVersionMapper {
    private RateVersionMapper() {}

    public static PackageRateVersionView toView(PackageRateVersion e) {
        return new PackageRateVersionView(
                e.getId(), e.getMaterialRate(), e.getLabourRate(), e.getOverheadRate());
    }
}
