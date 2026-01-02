package com.wd.api.model.enums;

/**
 * Represents the current phase of a construction project lifecycle.
 * Used to track project progression from initial design through completion and
 * warranty.
 */
public enum ProjectPhase {
    /**
     * Design phase - Creating architectural plans, approvals, design package
     * selection
     */
    DESIGN("Design", "Initial design and planning stage", 1),

    /**
     * Planning phase - Budget finalization, timeline creation, resource allocation
     */
    PLANNING("Planning", "Project planning and preparation", 2),

    /**
     * Execution phase - Active construction work in progress
     */
    EXECUTION("Execution", "Construction work in progress", 3),

    /**
     * Completion phase - Final inspections, handover preparation
     */
    COMPLETION("Completion", "Project nearing completion", 4),

    /**
     * Handover phase - Customer acceptance, final documentation
     */
    HANDOVER("Handover", "Project handover to customer", 5),

    /**
     * Warranty phase - Post-completion support and warranty period
     */
    WARRANTY("Warranty", "Post-completion warranty period", 6);

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
     * Check if this phase comes before another phase in the project lifecycle
     */
    public boolean isBefore(ProjectPhase other) {
        return this.order < other.order;
    }

    /**
     * Check if this phase comes after another phase in the project lifecycle
     */
    public boolean isAfter(ProjectPhase other) {
        return this.order > other.order;
    }
}
