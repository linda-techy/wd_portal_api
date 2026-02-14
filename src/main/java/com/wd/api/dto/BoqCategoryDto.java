package com.wd.api.dto;

import com.wd.api.model.BoqCategory;
import java.time.LocalDateTime;

public record BoqCategoryDto(
        Long id,
        Long projectId,
        Long parentId,
        String parentName,
        String name,
        String description,
        Integer displayOrder,
        Boolean isActive,
        int itemCount,
        LocalDateTime createdAt
) {

    public static BoqCategoryDto fromEntity(BoqCategory category, int itemCount) {
        return new BoqCategoryDto(
                category.getId(),
                category.getProject() != null ? category.getProject().getId() : null,
                category.getParent() != null ? category.getParent().getId() : null,
                category.getParent() != null ? category.getParent().getName() : null,
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getIsActive(),
                itemCount,
                category.getCreatedAt()
        );
    }
}
