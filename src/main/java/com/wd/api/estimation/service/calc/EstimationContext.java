package com.wd.api.estimation.service.calc;

import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.estimation.service.calc.view.*;

import java.math.BigDecimal;
import java.util.List;

public record EstimationContext(
        ProjectType projectType,
        EstimationPackageView pkg,
        PackageRateVersionView rateVersion,
        MarketIndexSnapshotView marketIndex,
        Dimensions dimensions,
        List<CustomisationChoice> customisations,
        List<SiteFeeApplied> siteFees,
        List<AddOnApplied> addOns,
        List<GovtFeeApplied> govtFees,
        BigDecimal discountPercent,
        BigDecimal gstRate,
        CalculationConstants constants) {}
