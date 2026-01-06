package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.DocumentResponse;
import com.wd.api.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*")
public class LeadDocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@PathVariable Long leadId) {
        List<DocumentResponse> docs = documentService.getDocuments(leadId, "LEAD");
        return ResponseEntity.ok(ApiResponse.success("Lead documents retrieved successfully", docs));
    }

    @PostMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long leadId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {

        DocumentResponse doc = documentService.uploadDocument(leadId, "LEAD", file, categoryId, description);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", doc));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }
}
