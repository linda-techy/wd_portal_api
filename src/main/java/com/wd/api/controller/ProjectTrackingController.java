package com.wd.api.controller;

import com.wd.api.model.*;
import com.wd.api.service.ProjectTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/tracking")
@RequiredArgsConstructor
public class ProjectTrackingController {

    private final ProjectTrackingService trackingService;

    // ===== PROJECT PHASES =====

    @GetMapping("/phases")
    public ResponseEntity<List<ProjectPhase>> getPhases(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getProjectPhases(projectId));
    }

    @PostMapping("/phases")
    public ResponseEntity<ProjectPhase> createPhase(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> request) {
        String phaseName = (String) request.get("phaseName");
        LocalDate plannedStart = request.get("plannedStart") != null
                ? LocalDate.parse((String) request.get("plannedStart"))
                : null;
        LocalDate plannedEnd = request.get("plannedEnd") != null ? LocalDate.parse((String) request.get("plannedEnd"))
                : null;
        Integer displayOrder = request.get("displayOrder") != null
                ? Integer.parseInt(request.get("displayOrder").toString())
                : null;

        ProjectPhase phase = trackingService.createPhase(projectId, phaseName, plannedStart, plannedEnd, displayOrder);
        return ResponseEntity.ok(phase);
    }

    @PutMapping("/phases/{phaseId}")
    public ResponseEntity<ProjectPhase> updatePhase(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @RequestBody Map<String, Object> request) {
        String status = (String) request.get("status");
        LocalDate actualStart = request.get("actualStart") != null
                ? LocalDate.parse((String) request.get("actualStart"))
                : null;
        LocalDate actualEnd = request.get("actualEnd") != null ? LocalDate.parse((String) request.get("actualEnd"))
                : null;

        ProjectPhase phase = trackingService.updatePhaseProgress(phaseId, status, actualStart, actualEnd);
        return ResponseEntity.ok(phase);
    }

    // ===== DELAY LOGS =====

    @GetMapping("/delays")
    public ResponseEntity<List<DelayLog>> getDelays(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getDelayLogs(projectId));
    }

    @PostMapping("/delays")
    public ResponseEntity<DelayLog> logDelay(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> request) {
        Long phaseId = request.get("phaseId") != null ? Long.parseLong(request.get("phaseId").toString()) : null;
        String delayType = (String) request.get("delayType");
        LocalDate fromDate = LocalDate.parse((String) request.get("fromDate"));
        LocalDate toDate = request.get("toDate") != null ? LocalDate.parse((String) request.get("toDate")) : null;
        String reason = (String) request.get("reason");
        Long loggedById = request.get("loggedById") != null ? Long.parseLong(request.get("loggedById").toString())
                : null;

        DelayLog log = trackingService.logDelay(projectId, phaseId, delayType, fromDate, toDate, reason, loggedById);
        return ResponseEntity.ok(log);
    }

    // ===== VARIATIONS =====

    @GetMapping("/variations")
    public ResponseEntity<List<ProjectVariation>> getVariations(@PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getVariations(projectId));
    }

    @PostMapping("/variations")
    public ResponseEntity<ProjectVariation> createVariation(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> request) {
        String description = (String) request.get("description");
        BigDecimal estimatedAmount = new BigDecimal(request.get("estimatedAmount").toString());
        Long createdById = request.get("createdById") != null ? Long.parseLong(request.get("createdById").toString())
                : null;

        ProjectVariation variation = trackingService.createVariation(projectId, description, estimatedAmount,
                createdById);
        return ResponseEntity.ok(variation);
    }

    @PutMapping("/variations/{variationId}/submit")
    public ResponseEntity<ProjectVariation> submitVariation(
            @PathVariable Long projectId,
            @PathVariable Long variationId) {
        return ResponseEntity.ok(trackingService.submitForApproval(variationId));
    }

    @PutMapping("/variations/{variationId}/approve")
    public ResponseEntity<ProjectVariation> approveVariation(
            @PathVariable Long projectId,
            @PathVariable Long variationId,
            @RequestBody Map<String, Object> request) {
        Long approvedById = Long.parseLong(request.get("approvedById").toString());
        boolean approve = Boolean.parseBoolean(request.get("approve").toString());

        return ResponseEntity.ok(trackingService.approveVariation(variationId, approvedById, approve));
    }

    // ===== HEALTH METRICS =====

    @GetMapping("/health")
    public ResponseEntity<ProjectTrackingService.ProjectHealthSummary> getProjectHealth(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(trackingService.getProjectHealth(projectId));
    }
}
