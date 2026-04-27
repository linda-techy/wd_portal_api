package com.wd.api.dto.dpc;

import com.wd.api.model.DpcDocumentScope;
import com.wd.api.model.DpcScopeOption;
import com.wd.api.model.DpcScopeTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for a single scope row inside a DPC document.
 *
 * The {@code brandsResolved} and {@code whatYouGetResolved} fields fall back
 * to the parent {@link DpcScopeTemplate} defaults when no per-document
 * override is set.  This is the value the renderer should display.
 */
public record DpcDocumentScopeDto(
        Long id,
        Long scopeTemplateId,
        String scopeCode,
        String scopeTitle,
        Long selectedOptionId,
        String selectedOptionCode,
        String selectedOptionDisplayName,
        String selectedOptionRationale,
        Map<String, String> brandsResolved,
        List<String> whatYouGetResolved,
        Boolean includedInPdf,
        Integer displayOrder,
        BigDecimal originalAmount,
        BigDecimal customizedAmount,
        List<DpcScopeOptionDto> availableOptions
) {

    /**
     * Build a scope DTO with brand and what-you-get overrides resolved against
     * template defaults.
     *
     * @param scope    the persisted scope row
     * @param template the parent scope template (must not be null)
     * @param options  the options the customer could have chosen for this scope
     * @param rollup   computed cost rollup for this scope (nullable — e.g. when
     *                 no BoQ items mapped); when null, amounts come through as
     *                 {@link BigDecimal#ZERO}
     */
    public static DpcDocumentScopeDto from(DpcDocumentScope scope,
                                           DpcScopeTemplate template,
                                           List<DpcScopeOption> options,
                                           DpcCostRollupDto rollup) {
        if (scope == null) return null;

        // Brands: prefer per-document override, fall back to template defaults.
        Map<String, String> brandsResolved = scope.getBrandsOverride() != null
                ? new HashMap<>(scope.getBrandsOverride())
                : (template != null && template.getDefaultBrands() != null
                        ? new HashMap<>(template.getDefaultBrands())
                        : new HashMap<>());

        // What-you-get: prefer per-document override, fall back to template list.
        List<String> whatYouGetResolved = scope.getWhatYouGetOverride() != null
                ? List.copyOf(scope.getWhatYouGetOverride())
                : (template != null && template.getWhatYouGet() != null
                        ? List.copyOf(template.getWhatYouGet())
                        : List.of());

        DpcScopeOption selected = scope.getSelectedOption();
        List<DpcScopeOptionDto> optionDtos = options == null
                ? List.of()
                : options.stream().map(DpcScopeOptionDto::from).collect(Collectors.toList());

        BigDecimal original = rollup != null ? rollup.originalAmount() : BigDecimal.ZERO;
        BigDecimal customized = rollup != null ? rollup.customizedAmount() : BigDecimal.ZERO;

        return new DpcDocumentScopeDto(
                scope.getId(),
                template != null ? template.getId() : null,
                template != null ? template.getCode() : null,
                template != null ? template.getTitle() : null,
                selected != null ? selected.getId() : null,
                selected != null ? selected.getCode() : null,
                selected != null ? selected.getDisplayName() : null,
                scope.getSelectedOptionRationale(),
                brandsResolved,
                whatYouGetResolved,
                scope.getIncludedInPdf(),
                scope.getDisplayOrder(),
                original,
                customized,
                optionDtos
        );
    }
}
