package com.wd.api.dto.dpc;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.DpcDocument;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full DTO for a DPC document.
 *
 * Bundles the persisted entity fields with computed children (scopes,
 * customizations, cost summary, payment milestones) and the project header
 * fields the renderer needs.
 */
public record DpcDocumentDto(
        // ---- Identity ----
        Long id,
        Long projectId,
        Long boqDocumentId,
        Integer revisionNumber,
        String status,

        // ---- Title / subtitle ----
        String titleOverride,
        String subtitleOverride,

        // ---- Project header ----
        String projectName,
        String projectLocation,
        String projectState,
        String projectDistrict,
        String projectType,
        BigDecimal sqfeet,
        Long customerId,

        // ---- Signatories ----
        String clientSignatoryName,
        String walldotSignatoryName,

        // ---- Walldot contacts ----
        Long projectEngineerUserId,
        String branchManagerName,
        String branchManagerPhone,
        String crmTeamName,
        String crmTeamPhone,

        // ---- Issue snapshot ----
        LocalDateTime issuedAt,
        Long issuedByUserId,
        Long issuedPdfDocumentId,

        // ---- Children (computed) ----
        List<DpcDocumentScopeDto> scopes,
        List<DpcCustomizationLineDto> customizationLines,
        DpcMasterCostSummaryDto masterCostSummary,
        List<DpcPaymentMilestoneDto> paymentMilestones,

        // ---- Audit ----
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DpcDocumentDto from(DpcDocument doc,
                                      CustomerProject project,
                                      List<DpcDocumentScopeDto> scopes,
                                      List<DpcCustomizationLineDto> customizations,
                                      DpcMasterCostSummaryDto summary,
                                      List<DpcPaymentMilestoneDto> milestones) {
        if (doc == null) return null;

        Long projectId = project != null ? project.getId()
                : (doc.getProject() != null ? doc.getProject().getId() : null);
        String projectName = project != null ? project.getName() : null;
        String projectLocation = project != null ? project.getLocation() : null;
        String projectState = project != null ? project.getState() : null;
        String projectDistrict = project != null ? project.getDistrict() : null;
        String projectType = project != null ? project.getProjectType() : null;
        BigDecimal sqfeet = project != null ? project.getSqfeet() : null;
        Long customerId = project != null ? project.getCustomerId() : null;

        return new DpcDocumentDto(
                doc.getId(),
                projectId,
                doc.getBoqDocument() != null ? doc.getBoqDocument().getId() : null,
                doc.getRevisionNumber(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getTitleOverride(),
                doc.getSubtitleOverride(),
                projectName,
                projectLocation,
                projectState,
                projectDistrict,
                projectType,
                sqfeet,
                customerId,
                doc.getClientSignatoryName(),
                doc.getWalldotSignatoryName(),
                doc.getProjectEngineerUserId(),
                doc.getBranchManagerName(),
                doc.getBranchManagerPhone(),
                doc.getCrmTeamName(),
                doc.getCrmTeamPhone(),
                doc.getIssuedAt(),
                doc.getIssuedByUserId(),
                doc.getIssuedPdfDocumentId(),
                scopes != null ? scopes : List.of(),
                customizations != null ? customizations : List.of(),
                summary,
                milestones != null ? milestones : List.of(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
