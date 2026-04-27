package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.dpc.AddCustomizationFromCatalogRequest;
import com.wd.api.dto.dpc.CreateDpcDocumentRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.DpcCustomizationLineDto;
import com.wd.api.dto.dpc.DpcDocumentDto;
import com.wd.api.dto.dpc.PromoteCustomizationToCatalogRequest;
import com.wd.api.dto.dpc.UpdateDpcDocumentRequest;
import com.wd.api.dto.dpc.UpdateDpcScopeRequest;
import com.wd.api.dto.dpc.UpsertCustomizationLineRequest;
import com.wd.api.model.DpcCustomizationLine;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.DpcDocumentRepository;
import com.wd.api.service.dpc.DpcCustomizationCatalogPromotionService;
import com.wd.api.service.dpc.DpcCustomizationService;
import com.wd.api.service.dpc.DpcDocumentService;
import com.wd.api.service.dpc.DpcIssueService;
import com.wd.api.service.dpc.DpcRenderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for DPC (Detailed Project Costing) documents.
 *
 * <p>Endpoints fall into three groups:
 * <ul>
 *   <li>Project-scoped: create, list, fetch latest by project.</li>
 *   <li>Document-scoped: read, patch header / scopes, manage customizations,
 *       render preview PDF, issue, branch a new revision.</li>
 *   <li>All endpoints are guarded by per-action DPC permissions
 *       ({@code DPC_VIEW}, {@code DPC_CREATE}, {@code DPC_EDIT},
 *       {@code DPC_ISSUE}).</li>
 * </ul>
 *
 * <p>Exception mapping:
 * <ul>
 *   <li>{@link IllegalStateException} -> 422 (state violation, e.g. BoQ not approved,
 *       DPC already issued).</li>
 *   <li>{@link IllegalArgumentException} -> 400 (validation, missing entity).</li>
 *   <li>any other exception -> 500.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class DpcDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DpcDocumentController.class);

    private final DpcDocumentService dpcDocumentService;
    private final DpcCustomizationService dpcCustomizationService;
    private final DpcRenderService dpcRenderService;
    private final DpcIssueService dpcIssueService;
    private final DpcDocumentRepository dpcDocumentRepository;
    private final DpcCustomizationCatalogPromotionService dpcCustomizationCatalogPromotionService;

    public DpcDocumentController(DpcDocumentService dpcDocumentService,
                                 DpcCustomizationService dpcCustomizationService,
                                 DpcRenderService dpcRenderService,
                                 DpcIssueService dpcIssueService,
                                 DpcDocumentRepository dpcDocumentRepository,
                                 DpcCustomizationCatalogPromotionService dpcCustomizationCatalogPromotionService) {
        this.dpcDocumentService = dpcDocumentService;
        this.dpcCustomizationService = dpcCustomizationService;
        this.dpcRenderService = dpcRenderService;
        this.dpcIssueService = dpcIssueService;
        this.dpcDocumentRepository = dpcDocumentRepository;
        this.dpcCustomizationCatalogPromotionService = dpcCustomizationCatalogPromotionService;
    }

    // ---- Project-scoped ----

    @PostMapping("/projects/{projectId}/dpc-documents")
    @PreAuthorize("hasAuthority('DPC_CREATE')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> create(
            @PathVariable Long projectId,
            @RequestBody(required = false) CreateDpcDocumentRequest request) {
        try {
            Long userId = getCurrentUserId();
            DpcDocumentDto dto = dpcDocumentService.create(projectId, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("DPC document created", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create DPC for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while creating the DPC"));
        }
    }

    @GetMapping("/projects/{projectId}/dpc-documents")
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<ApiResponse<List<DpcDocumentDto>>> listByProject(@PathVariable Long projectId) {
        try {
            List<DpcDocumentDto> dtos = dpcDocumentRepository
                    .findByProjectIdOrderByRevisionNumberDesc(projectId)
                    .stream()
                    .map(d -> dpcDocumentService.getById(d.getId()))
                    .toList();
            return ResponseEntity.ok(ApiResponse.success("DPC documents retrieved", dtos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to list DPC documents for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while listing DPC documents"));
        }
    }

    @GetMapping("/projects/{projectId}/dpc-documents/latest")
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> getLatest(@PathVariable Long projectId) {
        try {
            DpcDocumentDto dto = dpcDocumentService.getLatest(projectId);
            return ResponseEntity.ok(ApiResponse.success("Latest DPC retrieved", dto));
        } catch (IllegalArgumentException e) {
            // No DPC exists for this project yet.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch latest DPC for project {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while fetching the latest DPC"));
        }
    }

    // ---- Document-scoped ----

    @GetMapping("/dpc-documents/{id}")
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> getById(@PathVariable Long id) {
        try {
            DpcDocumentDto dto = dpcDocumentService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("DPC document retrieved", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while fetching the DPC"));
        }
    }

    @PatchMapping("/dpc-documents/{id}")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> updateHeader(
            @PathVariable Long id,
            @RequestBody UpdateDpcDocumentRequest request) {
        try {
            DpcDocumentDto dto = dpcDocumentService.updateHeader(id, request);
            return ResponseEntity.ok(ApiResponse.success("DPC document updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the DPC"));
        }
    }

    @PatchMapping("/dpc-documents/{id}/scopes/{scopeRowId}")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> updateScope(
            @PathVariable Long id,
            @PathVariable Long scopeRowId,
            @RequestBody UpdateDpcScopeRequest request) {
        try {
            DpcDocumentDto dto = dpcDocumentService.updateScope(id, scopeRowId, request);
            return ResponseEntity.ok(ApiResponse.success("DPC scope updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update DPC {} scope {}", id, scopeRowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the DPC scope"));
        }
    }

    // ---- Customization lines ----

    @PostMapping("/dpc-documents/{id}/customizations")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<DpcCustomizationLineDto>> addCustomizationLine(
            @PathVariable Long id,
            @RequestBody UpsertCustomizationLineRequest request) {
        try {
            DpcCustomizationLine line = dpcCustomizationService.addManualLine(id, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Customization line added", DpcCustomizationLineDto.from(line)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add customization line to DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while adding the customization line"));
        }
    }

    @PatchMapping("/dpc-documents/{id}/customizations/{lineId}")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<DpcCustomizationLineDto>> updateCustomizationLine(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody UpsertCustomizationLineRequest request) {
        try {
            DpcCustomizationLine line = dpcCustomizationService.updateLine(lineId, request);
            return ResponseEntity.ok(
                    ApiResponse.success("Customization line updated", DpcCustomizationLineDto.from(line)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update DPC {} customization line {}", id, lineId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the customization line"));
        }
    }

    @PostMapping("/dpc-documents/{id}/customizations/from-catalog")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<DpcCustomizationLineDto>> addCustomizationFromCatalog(
            @PathVariable Long id,
            @Valid @RequestBody AddCustomizationFromCatalogRequest request) {
        try {
            DpcCustomizationLine line = dpcCustomizationService.addCustomizationFromCatalog(id, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            "Customization line added from catalog",
                            DpcCustomizationLineDto.from(line)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add catalog customization to DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while adding the catalog customization"));
        }
    }

    @PostMapping("/dpc-documents/customizations/{lineId}/promote-to-catalog")
    @PreAuthorize("hasAnyAuthority('DPC_EDIT', 'DPC_CUSTOMIZATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<DpcCustomizationCatalogItemDto>> promoteCustomizationToCatalog(
            @PathVariable Long lineId,
            @Valid @RequestBody PromoteCustomizationToCatalogRequest request) {
        try {
            DpcCustomizationCatalogItemDto dto =
                    dpcCustomizationCatalogPromotionService.promoteAdHocCustomization(lineId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Customization promoted to catalog", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to promote customization line {} to catalog", lineId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while promoting the customization"));
        }
    }

    @DeleteMapping("/dpc-documents/{id}/customizations/{lineId}")
    @PreAuthorize("hasAuthority('DPC_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomizationLine(
            @PathVariable Long id,
            @PathVariable Long lineId) {
        try {
            dpcCustomizationService.deleteLine(lineId);
            return ResponseEntity.ok(ApiResponse.success("Customization line deleted", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete DPC {} customization line {}", id, lineId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while deleting the customization line"));
        }
    }

    // ---- Render / Issue / Revision ----

    @GetMapping("/dpc-documents/{id}/preview-pdf")
    @PreAuthorize("hasAuthority('DPC_VIEW')")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id) {
        try {
            byte[] bytes = dpcRenderService.renderPdf(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("dpc-preview-" + id + ".pdf").build());
            headers.setContentLength(bytes != null ? bytes.length : 0);
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (IllegalArgumentException e) {
            logger.warn("DPC {} not found for preview", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            logger.warn("DPC {} not in renderable state: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            logger.error("Failed to render preview PDF for DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/dpc-documents/{id}/issue")
    @PreAuthorize("hasAuthority('DPC_ISSUE')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> issue(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            DpcDocumentDto dto = dpcIssueService.issueAndPersist(id, userId);
            return ResponseEntity.ok(ApiResponse.success("DPC issued", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to issue DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while issuing the DPC"));
        }
    }

    @PostMapping("/dpc-documents/{id}/new-revision")
    @PreAuthorize("hasAuthority('DPC_CREATE')")
    public ResponseEntity<ApiResponse<DpcDocumentDto>> newRevision(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            DpcDocumentDto dto = dpcDocumentService.createNewRevision(id, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("New DPC revision created", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to branch new revision from DPC {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while branching the new revision"));
        }
    }

    // ---- Helpers ----

    /**
     * Resolve the current authenticated portal user's id, mirroring
     * {@code BoqController#getCurrentUserId()}.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof PortalUser portalUser) {
            return portalUser.getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication context");
    }
}
