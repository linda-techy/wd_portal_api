package com.wd.api.controller;

import com.wd.api.dto.*;
import com.wd.api.model.*;
import com.wd.api.service.ProjectTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tracking")
@RequiredArgsConstructor
public class ProjectTrackingController {

    private final ProjectTrackingService trackingService;

    // ===== PROJECT PHASES =====

    @GetMapping("/phases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectPhase>> getPhases(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getProjectPhases(projectId));
    }

    @PostMapping("/phases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectPhase> createPhase(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectPhaseRequest request) {
        ProjectPhase phase = trackingService.createPhase(
                projectId,
                request.phaseName(),
                request.plannedStart(),
                request.plannedEnd(),
                request.displayOrder());
        return ResponseEntity.ok(phase);
    }

    @PutMapping("/phases/{phaseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectPhase> updatePhase(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @Valid @RequestBody UpdatePhaseRequest request) {
        ProjectPhase phase = trackingService.updatePhaseProgress(
                phaseId,
                request.status(),
                request.actualStart(),
                request.actualEnd());
        return ResponseEntity.ok(phase);
    }

    // ===== DELAY LOGS =====

    @GetMapping("/delays")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DelayLog>> getDelays(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getDelayLogs(projectId));
    }

    @PostMapping("/delays")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DelayLog> logDelay(
            @PathVariable Long projectId,
            @Valid @RequestBody LogDelayRequest request) {
        DelayLog log = trackingService.logDelay(
                projectId,
                request.phaseId(),
                request.delayType(),
                request.fromDate(),
                request.toDate(),
                request.reason(),
                request.loggedById());
        return ResponseEntity.ok(log);
    }

    // ===== VARIATIONS =====

    @GetMapping("/variations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectVariation>> getVariations(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getVariations(projectId));
    }

    @PostMapping("/variations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectVariation> createVariation(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateVariationRequest request) {
        ProjectVariation variation = trackingService.createVariation(
                projectId,
                request.description(),
                request.estimatedAmount(),
                request.createdById());
        return ResponseEntity.ok(variation);
    }

    @PutMapping("/variations/{variationId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectVariation> submitVariation(
            @PathVariable Long projectId,
            @PathVariable Long variationId) {
        return ResponseEntity.ok(trackingService.submitForApproval(variationId));
    }

    @PutMapping("/variations/{variationId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectVariation> approveVariation(
            @PathVariable Long projectId,
            @PathVariable Long variationId,
            @Valid @RequestBody ApproveVariationRequest request) {
        return ResponseEntity.ok(trackingService.approveVariation(
                variationId,
                request.approvedById(),
                request.approve()));
    }

    // ===== HEALTH METRICS =====

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectTrackingService.ProjectHealthSummary> getProjectHealth(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getProjectHealth(projectId));
    }
}
