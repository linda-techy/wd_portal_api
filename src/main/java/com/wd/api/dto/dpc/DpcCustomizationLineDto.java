package com.wd.api.dto.dpc;

import com.wd.api.model.DpcCustomizationLine;

import java.math.BigDecimal;

/**
 * DTO for one DPC customization-line row (auto from BoQ ADDON or manual).
 */
public record DpcCustomizationLineDto(
        Long id,
        Integer displayOrder,
        String title,
        String description,
        BigDecimal amount,
        String source,
        Long boqItemId,
        Long customizationCatalogId
) {

    public static DpcCustomizationLineDto from(DpcCustomizationLine line) {
        if (line == null) return null;
        return new DpcCustomizationLineDto(
                line.getId(),
                line.getDisplayOrder(),
                line.getTitle(),
                line.getDescription(),
                line.getAmount(),
                line.getSource() != null ? line.getSource().name() : null,
                line.getBoqItemId(),
                line.getCatalogItem() != null ? line.getCatalogItem().getId() : null
        );
    }
}
