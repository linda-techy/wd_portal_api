package com.wd.api.model.enums;

/**
 * Represents the status of construction permits/approvals for a project.
 * Tracks the regulatory approval process required before construction can
 * begin.
 */
public enum PermitStatus {
    /**
     * No permit required for this type of project
     */
    NOT_REQUIRED("Not Required", "No regulatory permits needed"),

    /**
     * Permit application has been submitted and is pending approval
     */
    APPLIED("Applied", "Permit application submitted and pending"),

    /**
     * Permit has been approved by authorities
     */
    APPROVED("Approved", "Permit approved by regulatory authorities"),

    /**
     * Permit application was rejected
     */
    REJECTED("Rejected", "Permit application rejected"),

    /**
     * Permit is under review or requires modifications
     */
    UNDER_REVIEW("Under Review", "Permit application under review"),

    /**
     * Permit has expired and needs renewal
     */
    EXPIRED("Expired", "Permit validity has expired");

    private final String displayName;
    private final String description;

    PermitStatus(String displayName, String description) {
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
     * Check if construction can proceed with this permit status
     */
    public boolean canProceedWithConstruction() {
        return this == APPROVED || this == NOT_REQUIRED;
    }
}
