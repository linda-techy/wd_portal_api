package com.wd.api.controller;

import com.wd.api.dto.ProjectModuleDtos.*;
import com.wd.api.service.ProjectDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/customer-projects/{projectId}")
public class ProjectModuleController {

    private final ProjectDocumentService documentService;

    public ProjectModuleController(ProjectDocumentService documentService) {
        this.documentService = documentService;
    }

    // ===== DOCUMENT ENDPOINTS =====

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDocumentDto>> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String description,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        DocumentUploadRequest request = new DocumentUploadRequest(categoryId, description);
        ProjectDocumentDto doc = documentService.uploadDocument(projectId, file, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document uploaded successfully", doc));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> getDocuments(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long categoryId) {
        List<ProjectDocumentDto> docs = documentService.getProjectDocuments(projectId, categoryId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Documents retrieved successfully", docs));
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<ApiResponse<List<DocumentCategoryDto>>> getDocumentCategories(@PathVariable Long projectId) {
        List<DocumentCategoryDto> categories = documentService.getAllCategories();
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories retrieved successfully", categories));
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        try {
            if (auth.getPrincipal() instanceof com.wd.api.model.User) {
                return ((com.wd.api.model.User) auth.getPrincipal()).getId();
            }
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
