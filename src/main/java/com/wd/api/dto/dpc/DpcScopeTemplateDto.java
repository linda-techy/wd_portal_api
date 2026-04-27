package com.wd.api.dto.dpc;

import com.wd.api.model.DpcScopeOption;
import com.wd.api.model.DpcScopeTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for a DPC scope template (admin-managed content library row).
 *
 * Includes the embedded list of {@link DpcScopeOptionDto} options for the
 * "options considered" UI cards.
 */
public record DpcScopeTemplateDto(
        Long id,
        String code,
        Integer displayOrder,
        String title,
        String subtitle,
        String introParagraph,
        List<String> whatYouGet,
        List<String> qualityProcedures,
        List<String> documentsYouGet,
        List<String> boqCategoryPatterns,
        Map<String, String> defaultBrands,
        Boolean isActive,
        List<DpcScopeOptionDto> options
) {

    public static DpcScopeTemplateDto from(DpcScopeTemplate template, List<DpcScopeOption> options) {
        if (template == null) return null;
        List<DpcScopeOptionDto> optionDtos = options == null
                ? List.of()
                : options.stream().map(DpcScopeOptionDto::from).collect(Collectors.toList());
        return new DpcScopeTemplateDto(
                template.getId(),
                template.getCode(),
                template.getDisplayOrder(),
                template.getTitle(),
                template.getSubtitle(),
                template.getIntroParagraph(),
                template.getWhatYouGet(),
                template.getQualityProcedures(),
                template.getDocumentsYouGet(),
                template.getBoqCategoryPatterns(),
                template.getDefaultBrands(),
                template.getIsActive(),
                optionDtos
        );
    }
}
