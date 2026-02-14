package com.wd.api.controller;

import com.wd.api.dto.DocumentResponse;
import com.wd.api.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import com.wd.api.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/customer-projects/{projectId}")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class ProjectModuleController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectModuleController.class);

    private final DocumentService documentService;

    public ProjectModuleController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // ===== DOCUMENT ENDPOINTS =====

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String description) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required and cannot be empty"));
            }
            DocumentResponse doc = documentService.uploadDocument(projectId, "PROJECT", file, categoryId, description);
            return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", doc));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error uploading document for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to upload document for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to upload document"));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @PathVariable Long projectId) {
        try {
            List<DocumentResponse> docs = documentService.getDocuments(projectId, "PROJECT");
            return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", docs));
        } catch (Exception e) {
            logger.error("Failed to get documents for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve documents"));
        }
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<ApiResponse<List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto>>> getDocumentCategories(
            @PathVariable Long projectId) {
        try {
            List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto> categories = documentService
                    .getAllCategories("PROJECT");
            return ResponseEntity.ok(ApiResponse.success("Project document categories retrieved successfully", categories));
        } catch (Exception e) {
            logger.error("Failed to get document categories for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to retrieve document categories"));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Document not found for deletion - project: {}, document: {}", projectId, documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Document not found"));
        } catch (Exception e) {
            logger.error("Failed to delete document {} for project {}: {}", documentId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete document"));
        }
    }

    /**
     * Extract user ID from authentication context.
     * Returns null only if authentication is genuinely unavailable
     * (e.g., public endpoints). Callers should handle null appropriately.
     */
    @SuppressWarnings("unused")
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        if (auth.getPrincipal() instanceof com.wd.api.model.PortalUser) {
            return ((com.wd.api.model.PortalUser) auth.getPrincipal()).getId();
        }

        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            logger.warn("Invalid user ID format in auth principal: {}", auth.getName());
            return null;
        }
    }
}
