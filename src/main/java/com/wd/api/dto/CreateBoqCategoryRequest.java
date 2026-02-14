package com.wd.api.dto;

import jakarta.validation.constraints.*;

public record CreateBoqCategoryRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        Long parentId,

        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        Integer displayOrder
) {
}
