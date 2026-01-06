package com.wd.api.model.enums;

/**
 * Material Unit of Measurement
 * Standard construction industry units for material quantification
 */
public enum MaterialUnit {
    // Weight-based
    KG("Kilograms", "Weight in kilograms"),
    TONNES("Tonnes", "Weight in tonnes (1000 kg)"),
    QUINTAL("Quintal", "Weight in quintals (100 kg)"),

    // Volume-based
    BAGS("Bags", "Number of bags (e.g., cement bags)"),
    CUM("Cubic Meters", "Volume in cubic meters"),
    CFT("Cubic Feet", "Volume in cubic feet"),
    LITRES("Litres", "Volume in litres"),

    // Length-based
    METERS("Meters", "Length in meters"),
    FEET("Feet", "Length in feet"),
    RMT("Running Meters", "Running meters (for bars, pipes)"),

    // Area-based
    SQM("Square Meters", "Area in square meters"),
    SQFT("Square Feet", "Area in square feet"),

    // Count-based
    NOS("Numbers", "Count of items"),
    PAIRS("Pairs", "Count in pairs"),
    SETS("Sets", "Count in sets"),

    // Other
    LOADS("Loads", "Number of loads (trucks/vehicles)"),
    BUNDLE("Bundle", "Number of bundles");

    private final String displayName;
    private final String description;

    MaterialUnit(String displayName, String description) {
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
     * Check if this is a weight-based unit
     */
    public boolean isWeightBased() {
        return this == KG || this == TONNES || this == QUINTAL;
    }

    /**
     * Check if this is a volume-based unit
     */
    public boolean isVolumeBased() {
        return this == CUM || this == CFT || this == LITRES || this == BAGS;
    }

    /**
     * Check if this is a count-based unit
     */
    public boolean isCountBased() {
        return this == NOS || this == PAIRS || this == SETS;
    }
}
