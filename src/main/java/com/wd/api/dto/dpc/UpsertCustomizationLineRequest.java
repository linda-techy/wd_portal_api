package com.wd.api.dto.dpc;

import java.math.BigDecimal;

/**
 * Request body for creating a manual DPC customization line, or for editing
 * the title/description (and amount, when MANUAL) of an existing line.
 *
 * For AUTO_FROM_BOQ_ADDON lines the {@code amount} field is ignored — the
 * amount tracks the source BoQ item and refreshes via auto-populate.
 */
public record UpsertCustomizationLineRequest(
        Integer displayOrder,
        String title,
        String description,
        BigDecimal amount
) {
}
