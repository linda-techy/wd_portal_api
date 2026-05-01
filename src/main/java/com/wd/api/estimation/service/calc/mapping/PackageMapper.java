package com.wd.api.estimation.service.calc.mapping;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.service.calc.view.EstimationPackageView;

public final class PackageMapper {
    private PackageMapper() {}

    public static EstimationPackageView toView(EstimationPackage e) {
        return new EstimationPackageView(e.getId(), e.getInternalName(), e.getMarketingName());
    }
}
