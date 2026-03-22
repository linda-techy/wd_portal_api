package com.wd.api.model;

/**
 * Valid project type values for CustomerProject.projectType.
 * Stored as VARCHAR in the database — validated at service layer.
 *
 * RESIDENTIAL      — Individual home / villa construction
 * COMMERCIAL       — Office, retail, hospitality buildings
 * INDUSTRIAL       — Factory, warehouse, manufacturing facilities
 * RENOVATION       — Remodelling existing structure (residential or commercial)
 * INTERIOR_ONLY    — Interior design only, no structural work
 */
public enum ProjectType {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    RENOVATION,
    INTERIOR_ONLY;

    /**
     * Returns true if the given string is a valid ProjectType value (case-insensitive).
     */
    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;
        for (ProjectType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Default progress weights per project type.
     * Returns [milestoneWeight, taskWeight, budgetWeight] summing to 100.
     */
    public int[] progressWeights() {
        return switch (this) {
            case RESIDENTIAL    -> new int[]{50, 30, 20};
            case COMMERCIAL     -> new int[]{35, 40, 25};
            case INDUSTRIAL     -> new int[]{40, 35, 25};
            case RENOVATION     -> new int[]{45, 35, 20};
            case INTERIOR_ONLY  -> new int[]{60, 20, 20};
        };
    }
}
