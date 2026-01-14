package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Site Reports module
 * Extends base SearchFilterRequest with site-report-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SiteReportSearchFilter extends SearchFilterRequest {
    
    // Site Report-specific filters
    private Long projectId;          // Project ID
    private String reportType;       // Report type (DAILY, WEEKLY, INSPECTION, etc.)
    private Long reportedBy;         // Reporter user ID
    private Long reportedById;       // Reporter user ID (alias)
    private String location;         // Site location (partial match)
    private String title;            // Report title
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProject() {
        return projectId != null;
    }
    
    /**
     * Check if report type filter is applied
     */
    public boolean hasReportType() {
        return reportType != null && !reportType.trim().isEmpty();
    }
    
    /**
     * Check if location filter is applied
     */
    public boolean hasLocation() {
        return location != null && !location.trim().isEmpty();
    }
}

