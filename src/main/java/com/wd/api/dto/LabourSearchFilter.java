package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Labour module
 * Extends base SearchFilterRequest with labour-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LabourSearchFilter extends SearchFilterRequest {
    
    // Labour-specific filters
    private Long projectId;          // Project ID
    private String workerId;         // Worker ID (partial match)
    private String workerName;       // Worker name (partial match)
    private String workerRole;       // Worker role/skill
    private String contractorName;   // Contractor name (partial match)
    private Long contractorId;       // Contractor ID
    private String role;             // Worker role/skill
    private String employmentType;   // Employment type (DAILY_WAGE, CONTRACTOR, etc.)
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if contractor filter is applied
     */
    public boolean hasContractor() {
        return contractorName != null && !contractorName.trim().isEmpty();
    }
    
    /**
     * Check if role filter is applied
     */
    public boolean hasRole() {
        return role != null && !role.trim().isEmpty();
    }
}

