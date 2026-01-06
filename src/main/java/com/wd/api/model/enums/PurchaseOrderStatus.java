package com.wd.api.model.enums;

/**
 * Purchase Order Status Lifecycle
 * Represents the state of a purchase order from creation to completion
 */
public enum PurchaseOrderStatus {
    DRAFT("Draft", "PO created but not yet issued", 1),
    ISSUED("Issued", "PO sent to vendor, awaiting delivery", 2),
    PARTIALLY_RECEIVED("Partially Received", "Some items received via GRN", 3),
    RECEIVED("Received", "All items received via GRN", 4),
    CANCELLED("Cancelled", "PO cancelled before completion", 5),
    CLOSED("Closed", "PO completed and closed", 6);

    private final String displayName;
    private final String description;
    private final int order;

    PurchaseOrderStatus(String displayName, String description, int order) {
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
     * Check if PO can be edited
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /**
     * Check if PO is in active state (not terminal)
     */
    public boolean isActive() {
        return this != CANCELLED && this != CLOSED;
    }

    /**
     * Check if PO can receive goods
     */
    public boolean canReceiveGoods() {
        return this == ISSUED || this == PARTIALLY_RECEIVED;
    }

    /**
     * Check if later in lifecycle than another status
     */
    public boolean isAfter(PurchaseOrderStatus other) {
        return this.order > other.order;
    }
}
