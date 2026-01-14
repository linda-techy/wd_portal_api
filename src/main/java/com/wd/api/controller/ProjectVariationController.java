package com.wd.api.controller;

import com.wd.api.dto.ProjectVariationSearchFilter;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.ProjectVariationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/variations")
public class ProjectVariationController {

    @Autowired
    private ProjectVariationService variationService;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<ProjectVariation>> searchProjectVariations(@ModelAttribute ProjectVariationSearchFilter filter) {
        return ResponseEntity.ok(variationService.searchProjectVariations(filter));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Deprecated
    public ResponseEntity<List<ProjectVariation>> getVariations(@PathVariable Long projectId) {
        return ResponseEntity.ok(variationService.getVariationsByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ProjectVariation> createVariation(
            @PathVariable Long projectId,
            @RequestBody ProjectVariation variation,
            Authentication auth) {
        Long createdById = getCurrentUserId(auth);
        return ResponseEntity.ok(variationService.createVariation(variation, projectId, createdById));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ProjectVariation> updateVariation(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ProjectVariation variation) {
        return ResponseEntity.ok(variationService.updateVariation(id, variation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteVariation(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        variationService.deleteVariation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ProjectVariation> submitVariation(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        return ResponseEntity.ok(variationService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectVariation> approveVariation(
            @PathVariable Long projectId,
            @PathVariable Long id,
            Authentication auth) {
        Long approverId = getCurrentUserId(auth);
        return ResponseEntity.ok(variationService.approveVariation(id, approverId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectVariation> rejectVariation(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        Long rejectorId = getCurrentUserId(auth);
        String reason = payload.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(variationService.rejectVariation(id, rejectorId, reason));
    }

    private Long getCurrentUserId(Authentication auth) {
        PortalUser user = portalUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Portal User not found"));
        return user.getId();
    }
}
