package com.wd.api.dto.dpc;

import java.util.List;
import java.util.Map;

/**
 * PATCH request for an admin-managed DPC scope template.
 *
 * All fields nullable; only non-null fields are applied.  Setting a list/map
 * field to a non-null value REPLACES the existing collection in full —
 * partial-list edits are not supported here.
 */
public record UpdateScopeTemplateRequest(
        String title,
        String subtitle,
        String introParagraph,
        List<String> whatYouGet,
        List<String> qualityProcedures,
        List<String> documentsYouGet,
        List<String> boqCategoryPatterns,
        Map<String, String> defaultBrands,
        Boolean isActive
) {
}
