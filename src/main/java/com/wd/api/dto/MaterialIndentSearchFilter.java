package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Material Indents module
 * Extends base SearchFilterRequest with indent-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialIndentSearchFilter extends SearchFilterRequest {
    
    // Material Indent-specific filters
    private Long projectId;          // Project ID
    private String indentNumber;     // Indent number (partial match)
    private Long requestedBy;        // Requester user ID
    private Long approvedBy;         // Approver user ID
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if indent number filter is applied
     */
    public boolean hasIndentNumber() {
        return indentNumber != null && !indentNumber.trim().isEmpty();
    }
}

