package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * Search and filter request for Tasks module
 * Extends base SearchFilterRequest with task-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskSearchFilter extends SearchFilterRequest {
    
    // Task-specific filters
    private String priority;         // Priority (HIGH, MEDIUM, LOW)
    private Long assignedTo;         // Assigned user ID
    private Long projectId;          // Project ID
    private Long leadId;             // Lead ID
    private Long createdBy;          // Creator user ID
    
    // Due date range
    private LocalDate dueDateStart;
    private LocalDate dueDateEnd;
    
    /**
     * Check if due date range filter is applied
     */
    public boolean hasDueDateRange() {
        return dueDateStart != null || dueDateEnd != null;
    }
    
    /**
     * Check if assigned filter is applied
     */
    public boolean hasAssignedTo() {
        return assignedTo != null;
    }
    
    /**
     * Check if project filter is applied
     */
    public boolean hasProjectId() {
        return projectId != null;
    }
}

