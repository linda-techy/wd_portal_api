package com.wd.api.estimation.dto.admin;

import com.wd.api.estimation.domain.enums.PackageInternalName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PackageAdminCreateRequest(
        @NotNull PackageInternalName internalName,
        @NotBlank @Size(max = 100) String marketingName,
        @Size(max = 255) String tagline,
        String description,
        @NotNull @Min(1) @Max(999) Integer displayOrder) {}
