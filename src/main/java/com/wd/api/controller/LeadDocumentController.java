package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.DocumentResponse;
import com.wd.api.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
// CORS configuration is handled globally in SecurityConfig
public class LeadDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(LeadDocumentController.class);

    @Autowired
    private DocumentService documentService;

    @GetMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable Long leadId) {
        try {
            List<DocumentResponse> docs = documentService.getDocuments(leadId, "LEAD");
            return ResponseEntity.ok(ApiResponse.success("Lead documents retrieved successfully", docs));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for lead documents: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching documents for lead {}: {}", leadId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve documents: " + e.getMessage()));
        }
    }

    @PostMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long leadId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File is required and cannot be empty"));
            }

            DocumentResponse doc = documentService.uploadDocument(leadId, "LEAD", file, categoryId, description);
            return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", doc));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid document upload request for lead {}: {}", leadId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading document for lead {}: {}", leadId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid document deletion request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete document: " + e.getMessage()));
        }
    }

    /**
     * Get document categories for lead document uploads
     * Returns only LEAD-specific and BOTH categories
     */
    @GetMapping("/documents/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto>>> getDocumentCategories() {
        try {
            List<com.wd.api.dto.ProjectModuleDtos.DocumentCategoryDto> categories = documentService.getAllCategories("LEAD");
            return ResponseEntity.ok(ApiResponse.success("Lead document categories retrieved successfully", categories));
        } catch (Exception e) {
            logger.error("Error fetching document categories: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve document categories: " + e.getMessage()));
        }
    }
}
