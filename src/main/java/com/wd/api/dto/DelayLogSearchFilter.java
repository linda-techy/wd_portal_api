package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Delay Logs module
 * Extends base SearchFilterRequest with delay-log-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DelayLogSearchFilter extends SearchFilterRequest {
    
    // Delay Log-specific filters
    private Long projectId;          // Project ID
    private String delayType;        // Delay type (WEATHER, MATERIAL, LABOUR, etc.)
    private Long loggedById;         // User who logged the delay
    private String severity;         // Severity (LOW, MEDIUM, HIGH, CRITICAL)
    private Boolean activeOnly;      // Show only active/open delays (toDate is null)
    private Boolean closedOnly;      // Show only closed delays (toDate is not null)
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if delay type filter is applied
     */
    public boolean hasDelayType() {
        return delayType != null && !delayType.trim().isEmpty();
    }
    
    /**
     * Check if active only filter is applied
     */
    public boolean isActiveOnly() {
        return activeOnly != null && activeOnly;
    }
    
    /**
     * Check if closed only filter is applied
     */
    public boolean isClosedOnly() {
        return closedOnly != null && closedOnly;
    }
}

