package com.wd.api.model.enums;

/**
 * Represents the current phase of a construction project lifecycle.
 * Standardized to 5 phases: Planning, Design, Construction, Completed, On Hold.
 */
public enum ProjectPhase {
    PLANNING("Planning", "Budget, timeline & preparation", 1),
    DESIGN("Design", "Plans, approvals & design package", 2),
    CONSTRUCTION("Construction", "Active construction on site", 3),
    COMPLETED("Completed", "Project completed & handed over", 4),
    ON_HOLD("On Hold", "Project temporarily paused", 5);

    private final String displayName;
    private final String description;
    private final int order;

    ProjectPhase(String displayName, String description, int order) {
        this.displayName = displayName;
        this.description = description;
        this.order = order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    /**
     * Check if this phase comes before another phase in the project lifecycle.
     * Note: ON_HOLD is a special state and not part of the linear progression.
     */
    public boolean isBefore(ProjectPhase other) {
        return this.order < other.order;
    }

    /**
     * Check if this phase comes after another phase in the project lifecycle.
     */
    public boolean isAfter(ProjectPhase other) {
        return this.order > other.order;
    }
}
