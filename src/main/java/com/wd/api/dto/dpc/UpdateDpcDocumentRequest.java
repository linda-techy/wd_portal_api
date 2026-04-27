package com.wd.api.dto.dpc;

/**
 * PATCH request for DPC document header fields.
 *
 * All fields are nullable.  Only non-null fields are applied to the entity;
 * null fields leave the existing value untouched.  Mutating an ISSUED DPC
 * is rejected at the service layer.
 */
public record UpdateDpcDocumentRequest(
        String titleOverride,
        String subtitleOverride,
        String clientSignatoryName,
        String walldotSignatoryName,
        Long projectEngineerUserId,
        String branchManagerName,
        String branchManagerPhone,
        String crmTeamName,
        String crmTeamPhone
) {
}
