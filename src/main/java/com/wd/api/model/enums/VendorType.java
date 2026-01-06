package com.wd.api.model.enums;

/**
 * Vendor Type Classification
 * Categorizes vendors based on the primary goods/services they provide
 */
public enum VendorType {
    MATERIAL("Material Supplier", "Supplies construction materials (cement, steel, bricks, etc.)"),
    LABOUR("Labour Contractor", "Provides skilled/unskilled labour"),
    EQUIPMENT("Equipment Rental", "Provides construction equipment and machinery"),
    SERVICES("Service Provider", "Provides specialized services (plumbing, electrical, etc.)"),
    SUBCONTRACTOR("Subcontractor", "Takes on entire work packages"),
    CONSULTANT("Consultant", "Provides professional consulting services");

    private final String displayName;
    private final String description;

    VendorType(String displayName, String description) {
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
     * Check if this vendor type requires material tracking
     */
    public boolean requiresMaterialTracking() {
        return this == MATERIAL;
    }

    /**
     * Check if this vendor type requires attendance tracking
     */
    public boolean requiresAttendanceTracking() {
        return this == LABOUR;
    }
}
