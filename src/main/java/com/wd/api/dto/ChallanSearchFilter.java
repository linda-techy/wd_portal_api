package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Challans module
 * Extends base SearchFilterRequest with challan-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChallanSearchFilter extends SearchFilterRequest {
    
    // Challan-specific filters
    private Long projectId;          // Project ID
    private String challanNumber;    // Challan number (partial match)
    private String challanType;      // Type (INWARD, OUTWARD, RETURN)
    private Long supplierId;         // Supplier/Vendor ID
    private String vehicleNumber;    // Vehicle number (partial match)
    private Boolean acknowledged;    // Acknowledgement flag
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if challan type filter is applied
     */
    public boolean hasChallanType() {
        return challanType != null && !challanType.trim().isEmpty();
    }
}

