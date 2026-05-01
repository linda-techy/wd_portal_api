package com.wd.api.estimation.dto.admin;

import com.wd.api.estimation.domain.EstimationPackage;
import com.wd.api.estimation.domain.enums.PackageInternalName;

import java.util.UUID;

public record PackageAdminResponse(
        UUID id,
        PackageInternalName internalName,
        String marketingName,
        String tagline,
        String description,
        Integer displayOrder,
        boolean active) {

    public static PackageAdminResponse fromEntity(EstimationPackage p) {
        return new PackageAdminResponse(
                p.getId(), p.getInternalName(), p.getMarketingName(),
                p.getTagline(), p.getDescription(), p.getDisplayOrder(), p.isActive());
    }
}
