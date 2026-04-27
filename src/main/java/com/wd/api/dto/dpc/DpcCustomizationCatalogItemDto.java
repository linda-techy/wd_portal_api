package com.wd.api.dto.dpc;

import com.wd.api.model.DpcCustomizationCatalogItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read DTO for a DPC-customization-catalog row.
 */
public record DpcCustomizationCatalogItemDto(
        Long id,
        String code,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal defaultAmount,
        Integer timesUsed,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DpcCustomizationCatalogItemDto from(DpcCustomizationCatalogItem entity) {
        if (entity == null) return null;
        return new DpcCustomizationCatalogItemDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getUnit(),
                entity.getDefaultAmount(),
                entity.getTimesUsed(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
