package com.wd.api.dto.dpc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to add a new "options considered" card under a DPC scope template.
 *
 * <p>{@code displayOrder} is optional — when null, the service appends the new
 * option at the end (max existing + 1).
 *
 * <p>{@code imagePath} is optional. v1 accepts a URL the admin pastes; image
 * upload is a separate slice.
 */
public record CreateScopeOptionRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 500) String imagePath,
        Integer displayOrder
) {
}
