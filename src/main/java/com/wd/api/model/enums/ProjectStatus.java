package com.wd.api.model.enums;

/**
 * Represents the overall operational status of a project.
 * Used to track if a project is currently active or has been
 * terminated/suspended.
 */
public enum ProjectStatus {
    /**
     * Project is currently active and ongoing
     */
    ACTIVE("Active", "Project is currently active"),

    /**
     * Project has been successfully completed
     */
    COMPLETED("Completed", "Project completed successfully"),

    /**
     * Project work is temporarily suspended
     */
    SUSPENDED("Suspended", "Project temporarily suspended"),

    /**
     * Project has been cancelled
     */
    CANCELLED("Cancelled", "Project cancelled"),

    /**
     * Project is on hold awaiting decisions/approvals
     */
    ON_HOLD("On Hold", "Project on hold pending decisions");

    private final String displayName;
    private final String description;

    ProjectStatus(String displayName, String description) {
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
     * Check if project is in an operational state (not terminated)
     */
    public boolean isOperational() {
        return this == ACTIVE || this == SUSPENDED || this == ON_HOLD;
    }

    /**
     * Check if project is in a terminal state (completed or cancelled)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
