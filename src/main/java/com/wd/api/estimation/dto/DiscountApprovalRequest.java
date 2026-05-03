package com.wd.api.estimation.dto;

/**
 * O — Body for approve-discount / reject-discount endpoints. Notes are optional
 * on approval but required on rejection (enforced in the service).
 */
public record DiscountApprovalRequest(String notes) {}
