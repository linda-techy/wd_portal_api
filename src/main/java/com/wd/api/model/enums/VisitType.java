package com.wd.api.model.enums;

/**
 * Type of site visit - identifies the role of the person visiting
 */
public enum VisitType {
    SITE_ENGINEER("Site Engineer", "Daily site supervision"),
    PROJECT_MANAGER("Project Manager", "Project oversight visits"),
    SUPERVISOR("Supervisor", "Work supervision"),
    CONTRACTOR("Contractor", "Contractor site work"),
    CLIENT("Client", "Client inspection visit"),
    GENERAL("General", "General site visit");

    private final String displayName;
    private final String description;

    VisitType(String displayName, String description) {
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
     * Check if this visit type requires mandatory check-out
     */
    public boolean requiresMandatoryCheckout() {
        return this == SITE_ENGINEER || this == PROJECT_MANAGER || this == SUPERVISOR;
    }
}
