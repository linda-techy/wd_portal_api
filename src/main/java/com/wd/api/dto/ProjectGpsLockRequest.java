package com.wd.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /customer-projects/{id}/gps — captures the
 * device's current GPS reading and stamps it as the project's site
 * location (V82). Bounds checks reject obviously-bad inputs at the API
 * layer; the service layer additionally rejects (0, 0) "Null Island".
 */
public record ProjectGpsLockRequest(
        @NotNull
        @DecimalMin(value = "-90.0",  message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0",   message = "Latitude must be between -90 and 90")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
        Double longitude
) {
}
