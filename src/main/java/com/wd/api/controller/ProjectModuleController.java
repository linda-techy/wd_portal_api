package com.wd.api.controller;

import com.wd.api.dto.DocumentResponse;
import com.wd.api.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import com.wd.api.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/customer-projects/{projectId}")
public class ProjectModuleController {

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
        DocumentResponse doc = documentService.uploadDocument(projectId, "PROJECT", file, categoryId, description);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", doc));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @PathVariable Long projectId) {
        List<DocumentResponse> docs = documentService.getDocuments(projectId, "PROJECT");
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", docs));
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<ApiResponse<List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto>>> getDocumentCategories(
            @PathVariable Long projectId) {
        // Reuse existing categories but map through new service if needed
        // For now, I'll keep the category service separate or move it
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", null));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        try {
            if (auth.getPrincipal() instanceof com.wd.api.model.PortalUser) {
                return ((com.wd.api.model.PortalUser) auth.getPrincipal()).getId();
            }
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
