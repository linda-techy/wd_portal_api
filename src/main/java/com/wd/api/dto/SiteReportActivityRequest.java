package com.wd.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request payload for one row of a site report's activity breakdown
 * (V84). Accepted as part of the replace-all save list — staff submits
 * the full list each time and the service deletes the previous set
 * before inserting these rows.
 */
public record SiteReportActivityRequest(
        @NotBlank @Size(max = 150) String name,
        @PositiveOrZero Integer manpower,
        String equipment,
        String notes
) {
}
