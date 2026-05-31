package com.wd.api.dto;

import com.wd.api.model.QualityCheck;

import java.time.LocalDateTime;

/**
 * Inbound request body for updating a {@link QualityCheck}.
 *
 * <p>Binding the JPA entity directly as a {@code @RequestBody} (the previous
 * approach) is a mass-assignment risk (SonarQube java:S4684): a client could set
 * server-managed columns such as {@code id}, {@code createdAt} or {@code updatedAt}.
 * This DTO exposes only the client-settable fields and maps to a transient entity;
 * the FK relations {@code project} and {@code conductedBy} are resolved server-side.
 */
public record QualityCheckRequest(
        String title,
        String description,
        LocalDateTime checkDate,
        String status,
        String result,
        String remarks
) {
    /** Map to a transient entity, preserving the exact (null-passthrough) binding the entity used. */
    public QualityCheck toEntity() {
        QualityCheck c = new QualityCheck();
        c.setTitle(title);
        c.setDescription(description);
        c.setCheckDate(checkDate);
        c.setStatus(status);
        c.setResult(result);
        c.setRemarks(remarks);
        return c;
    }
}
