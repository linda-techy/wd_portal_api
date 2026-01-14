package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Lead Interactions module
 * Extends base SearchFilterRequest with interaction-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadInteractionSearchFilter extends SearchFilterRequest {
    
    // Lead Interaction-specific filters
    private Long leadId;             // Lead ID
    private String interactionType;  // Type (CALL, EMAIL, MEETING, VISIT, etc.)
    private Long userId;             // User who performed the interaction
    private String outcome;          // Outcome (POSITIVE, NEUTRAL, NEGATIVE, FOLLOW_UP_REQUIRED)
    private Boolean followUpRequired; // Follow-up flag
    
    /**
     * Check if lead filter is applied
     */
    public boolean hasLead() {
        return leadId != null;
    }
    
    /**
     * Check if interaction type filter is applied
     */
    public boolean hasInteractionType() {
        return interactionType != null && !interactionType.trim().isEmpty();
    }
}

