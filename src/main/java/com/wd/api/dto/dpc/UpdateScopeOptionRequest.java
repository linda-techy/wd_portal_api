package com.wd.api.dto.dpc;

import jakarta.validation.constraints.Size;

/** Patch-shape for an existing scope option — every field nullable. */
public record UpdateScopeOptionRequest(
        @Size(max = 50) String code,
        @Size(max = 100) String displayName,
        @Size(max = 500) String imagePath,
        Integer displayOrder,
        Boolean isActive
) {
}
