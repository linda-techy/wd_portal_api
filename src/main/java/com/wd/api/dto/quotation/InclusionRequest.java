package com.wd.api.dto.quotation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a {@code quotation_inclusions} row.
 *
 * <p>{@code displayOrder} is nullable on create — service appends to the end
 * (max + 1) when omitted, so the typical "add another inclusion" call from
 * the Flutter form is a one-field POST.
 */
public record InclusionRequest(
        @PositiveOrZero Integer displayOrder,
        @Size(max = 50) String category,
        @NotBlank String text
) {
}
