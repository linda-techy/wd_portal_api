package com.wd.api.dto;

import com.wd.api.model.SiteReportActivity;

/**
 * Read DTO for one row of a site report's activity breakdown (V84).
 * Flat shape so the Flutter side parses cleanly without diving into
 * nested entity proxies.
 */
public record SiteReportActivityResponse(
        Long id,
        String name,
        Integer manpower,
        String equipment,
        String notes,
        Integer displayOrder
) {
    public static SiteReportActivityResponse from(SiteReportActivity row) {
        return new SiteReportActivityResponse(
                row.getId(),
                row.getName(),
                row.getManpower(),
                row.getEquipment(),
                row.getNotes(),
                row.getDisplayOrder()
        );
    }
}
