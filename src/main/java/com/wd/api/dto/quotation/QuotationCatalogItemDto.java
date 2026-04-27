package com.wd.api.dto.quotation;

import com.wd.api.model.QuotationCatalogItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read DTO for a quotation-catalog row.
 */
public record QuotationCatalogItemDto(
        Long id,
        String code,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal defaultUnitPrice,
        Integer timesUsed,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static QuotationCatalogItemDto from(QuotationCatalogItem entity) {
        if (entity == null) return null;
        return new QuotationCatalogItemDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getUnit(),
                entity.getDefaultUnitPrice(),
                entity.getTimesUsed(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
