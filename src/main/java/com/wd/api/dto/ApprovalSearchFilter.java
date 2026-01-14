package com.wd.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Search and filter request for Approvals module
 * Extends base SearchFilterRequest with approval-specific filters
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalSearchFilter extends SearchFilterRequest {
    
    // Approval-specific filters
    private String approverType;     // Approver type (USER, ROLE, etc.)
    private String moduleType;       // Module type (INDENT, PO, PAYMENT, etc.)
    private Long approverId;         // Approver ID
    private Long requesterId;        // Requester ID
    private Long referenceId;        // Reference ID (target ID)
    
    /**
     * Check if approver type filter is applied
     */
    public boolean hasApproverType() {
        return approverType != null && !approverType.trim().isEmpty();
    }
    
    /**
     * Check if module type filter is applied
     */
    public boolean hasModuleType() {
        return moduleType != null && !moduleType.trim().isEmpty();
    }
}

