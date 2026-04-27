package com.wd.api.dto.dpc;

import java.util.List;
import java.util.Map;

/**
 * PATCH request for one scope row inside a DPC document.
 *
 * All fields are nullable; null leaves the existing value unchanged.
 * Setting {@code brandsOverride} or {@code whatYouGetOverride} to a non-null
 * value installs the override; passing them as null does NOT clear the
 * override (that would require a separate explicit clear request).
 */
public record UpdateDpcScopeRequest(
        Long selectedOptionId,
        String selectedOptionRationale,
        Map<String, String> brandsOverride,
        List<String> whatYouGetOverride,
        Boolean includedInPdf
) {
}
