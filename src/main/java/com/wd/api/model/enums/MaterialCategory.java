package com.wd.api.model.enums;

/**
 * Material Category Classification
 * Categorizes construction materials for better organization and reporting
 */
public enum MaterialCategory {
    CEMENT("Cement & Concrete", "Cement, ready-mix concrete, admixtures"),
    STEEL("Steel & Reinforcement", "TMT bars, structural steel, mesh"),
    AGGREGATE("Aggregates", "Sand, gravel, crushed stone, M-sand"),
    BRICKS_BLOCKS("Bricks & Blocks", "Clay bricks, concrete blocks, AAC blocks"),
    ELECTRICAL("Electrical", "Wires, switches, fixtures, panels"),
    PLUMBING("Plumbing", "Pipes, fittings, fixtures, sanitary ware"),
    PAINT_FINISHING("Paint & Finishing", "Paints, putty, tiles, flooring"),
    WOOD_TIMBER("Wood & Timber", "Timber, plywood, doors, windows"),
    HARDWARE("Hardware", "Nails, screws, hinges, locks"),
    WATERPROOFING("Waterproofing", "Waterproofing chemicals, membranes"),
    MISCELLANEOUS("Miscellaneous", "Other materials");

    private final String displayName;
    private final String description;

    MaterialCategory(String displayName, String description) {
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
     * Check if this category requires special storage
     */
    public boolean requiresSpecialStorage() {
        return this == CEMENT || this == PAINT_FINISHING || this == WATERPROOFING;
    }

    /**
     * Check if this category is subject to wastage tracking
     */
    public boolean requiresWastageTracking() {
        return this == CEMENT || this == STEEL || this == AGGREGATE;
    }
}
