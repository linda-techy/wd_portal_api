package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Quality Checks module
 * Extends base SearchFilterRequest with quality-check-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckSearchFilter extends SearchFilterRequest {
    
    // Quality Check-specific filters
    private Long projectId;          // Project ID
    private String checkType;        // Type of quality check
    private String result;           // Result (PASSED, FAILED, PENDING)
    private Long inspectorId;        // Inspector user ID
    private String area;             // Area/location of check
    private String checklistName;    // Checklist/title name
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if result filter is applied
     */
    public boolean hasResult() {
        return result != null && !result.trim().isEmpty();
    }
    
    /**
     * Check if inspector filter is applied
     */
    public boolean hasInspector() {
        return inspectorId != null;
    }
}

