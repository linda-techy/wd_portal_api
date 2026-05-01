package com.wd.api.estimation.service.calc.view;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import java.util.UUID;

public record EstimationPackageView(UUID id, PackageInternalName internalName, String marketingName) {}
