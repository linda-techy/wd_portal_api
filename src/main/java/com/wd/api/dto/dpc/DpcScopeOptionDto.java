package com.wd.api.dto.dpc;

import com.wd.api.model.DpcScopeOption;

/**
 * DTO for one "options considered" card under a DPC scope.
 *
 * Mirrors {@link com.wd.api.model.DpcScopeOption} for read endpoints.
 */
public record DpcScopeOptionDto(
        Long id,
        Long scopeTemplateId,
        String code,
        String displayName,
        String imagePath,
        Integer displayOrder
) {

    public static DpcScopeOptionDto from(DpcScopeOption option) {
        if (option == null) return null;
        return new DpcScopeOptionDto(
                option.getId(),
                option.getScopeTemplate() != null ? option.getScopeTemplate().getId() : null,
                option.getCode(),
                option.getDisplayName(),
                option.getImagePath(),
                option.getDisplayOrder()
        );
    }
}
