package com.wd.api.dto;

import com.wd.api.model.SiteVisitViolation;

import java.time.LocalDateTime;

/**
 * Portal-internal view of a geofence violation. Never returned by customer-api.
 */
public record SiteVisitViolationDTO(
        Long id,
        Long projectId,
        String projectName,
        Long userId,
        String userName,
        String userEmail,
        String attemptType,
        LocalDateTime attemptedAt,
        Double attemptedLatitude,
        Double attemptedLongitude,
        Double projectLatitude,
        Double projectLongitude,
        Double distanceKm,
        Double allowedRadiusKm,
        Long visitId,
        String errorMessage
) {
    public static SiteVisitViolationDTO from(SiteVisitViolation v) {
        var project = v.getProject();
        var user = v.getUser();
        return new SiteVisitViolationDTO(
                v.getId(),
                project != null ? project.getId() : null,
                project != null ? project.getName() : null,
                user != null ? user.getId() : null,
                fullName(user),
                user != null ? user.getEmail() : null,
                v.getAttemptType() != null ? v.getAttemptType().name() : null,
                v.getAttemptedAt(),
                v.getAttemptedLatitude(),
                v.getAttemptedLongitude(),
                v.getProjectLatitude(),
                v.getProjectLongitude(),
                v.getDistanceKm(),
                v.getAllowedRadiusKm(),
                v.getVisitId(),
                v.getErrorMessage()
        );
    }

    /** Null-safe full-name composition. Returns null if no name parts are populated. */
    private static String fullName(com.wd.api.model.PortalUser user) {
        if (user == null) return null;
        String first = user.getFirstName();
        String last = user.getLastName();
        if ((first == null || first.isBlank()) && (last == null || last.isBlank())) {
            return null;
        }
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }
}
