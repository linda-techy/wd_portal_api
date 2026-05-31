package com.wd.api.dto;

import com.wd.api.model.DesignPackageTemplate;

import java.math.BigDecimal;

/**
 * Inbound request body for creating/updating a {@link DesignPackageTemplate}.
 *
 * <p>Binding the JPA entity directly as a {@code @RequestBody} (the previous
 * approach) is a mass-assignment risk (SonarQube java:S4684): a client could set
 * server-managed columns such as {@code id}, {@code createdAt}, {@code updatedAt},
 * {@code createdByUserId} or {@code updatedByUserId}. This DTO exposes only the
 * client-settable fields and maps to a transient entity; the audit user ids are
 * stamped server-side from the authenticated principal.
 */
public record DesignPackageTemplateRequest(
        String code,
        String name,
        String tagline,
        String description,
        BigDecimal ratePerSqft,
        BigDecimal fullPaymentDiscountPct,
        Integer revisionsIncluded,
        String features,
        Integer displayOrder,
        Boolean isActive
) {
    /** Map to a transient entity, preserving the exact (null-passthrough) binding the entity used. */
    public DesignPackageTemplate toEntity() {
        DesignPackageTemplate t = new DesignPackageTemplate();
        t.setCode(code);
        t.setName(name);
        t.setTagline(tagline);
        t.setDescription(description);
        t.setRatePerSqft(ratePerSqft);
        t.setFullPaymentDiscountPct(fullPaymentDiscountPct);
        t.setRevisionsIncluded(revisionsIncluded);
        t.setFeatures(features);
        t.setDisplayOrder(displayOrder);
        t.setIsActive(isActive);
        return t;
    }
}
