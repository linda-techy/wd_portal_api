package com.wd.api.controller;

import com.wd.api.model.ProjectVariation;
import com.wd.api.service.ProjectVariationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/variations")
public class ProjectVariationController {

    @Autowired
    private ProjectVariationService variationService;

    @GetMapping
    public ResponseEntity<List<ProjectVariation>> getVariations(@PathVariable Long projectId) {
        return ResponseEntity.ok(variationService.getVariationsByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectVariation> createVariation(
            @PathVariable Long projectId,
            @RequestBody ProjectVariation variation) {
        // TODO: Get real user ID from security context
        Long createdById = 1L;
        return ResponseEntity.ok(variationService.createVariation(variation, projectId, createdById));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectVariation> updateVariation(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ProjectVariation variation) {
        return ResponseEntity.ok(variationService.updateVariation(id, variation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVariation(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        variationService.deleteVariation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ProjectVariation> submitVariation(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        return ResponseEntity.ok(variationService.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ProjectVariation> approveVariation(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        // TODO: Get real user ID
        Long approverId = 1L;
        return ResponseEntity.ok(variationService.approveVariation(id, approverId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ProjectVariation> rejectVariation(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        // TODO: Get real user ID
        Long rejectorId = 1L;
        String reason = payload.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(variationService.rejectVariation(id, rejectorId, reason));
    }
}
