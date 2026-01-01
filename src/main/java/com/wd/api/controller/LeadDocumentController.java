package com.wd.api.controller;

import com.wd.api.model.LeadDocument;
import com.wd.api.service.LeadDocumentService;
import com.wd.api.model.User;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*")
public class LeadDocumentController {

    @Autowired
    private LeadDocumentService leadDocumentService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<LeadDocument>> getDocuments(@PathVariable Long leadId) {
        return ResponseEntity.ok(leadDocumentService.getDocumentsByLeadId(leadId));
    }

    @PostMapping("/{leadId}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<LeadDocument> uploadDocument(
            @PathVariable Long leadId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(leadDocumentService.uploadDocument(leadId, file, description, category, user.getId()));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        leadDocumentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
