package com.wd.api.estimation.dto.admin;

import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.ProjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RateVersionResponse(
        UUID id,
        UUID packageId,
        ProjectType projectType,
        BigDecimal materialRate,
        BigDecimal labourRate,
        BigDecimal overheadRate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {

    public static RateVersionResponse fromEntity(PackageRateVersion rv) {
        return new RateVersionResponse(
                rv.getId(), rv.getPackageId(), rv.getProjectType(),
                rv.getMaterialRate(), rv.getLabourRate(), rv.getOverheadRate(),
                rv.getEffectiveFrom(), rv.getEffectiveTo());
    }
}
