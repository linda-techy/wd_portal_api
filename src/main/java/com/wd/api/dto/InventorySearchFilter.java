package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Inventory module
 * Extends base SearchFilterRequest with inventory-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventorySearchFilter extends SearchFilterRequest {
    
    // Inventory-specific filters
    private String materialType;     // Material type/category
    private String materialCode;     // Material code (partial match)
    private Long projectId;          // Project ID (for project-wise stock)
    private Boolean lowStock;        // Filter for low stock items
    private Boolean active;          // Filter for active/inactive materials
    
    // Quantity range
    private Double minQuantity;
    private Double maxQuantity;
    
    /**
     * Check if quantity range filter is applied
     */
    public boolean hasQuantityRange() {
        return minQuantity != null || maxQuantity != null;
    }
    
    /**
     * Check if material type filter is applied
     */
    public boolean hasMaterialType() {
        return materialType != null && !materialType.trim().isEmpty();
    }
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
}

