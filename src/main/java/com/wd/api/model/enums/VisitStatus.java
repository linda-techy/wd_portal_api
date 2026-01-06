package com.wd.api.model.enums;

/**
 * Status of a site visit lifecycle
 */
public enum VisitStatus {
    PENDING("Pending", "Visit scheduled but not started"),
    CHECKED_IN("Checked In", "Currently on site"),
    CHECKED_OUT("Checked Out", "Visit completed"),
    CANCELLED("Cancelled", "Visit was cancelled");

    private final String displayName;
    private final String description;

    VisitStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this status allows check-in
     */
    public boolean canCheckIn() {
        return this == PENDING;
    }

    /**
     * Check if this status allows check-out
     */
    public boolean canCheckOut() {
        return this == CHECKED_IN;
    }
}
