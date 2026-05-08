package com.wd.api.controller;

import com.wd.api.dto.CrCostRequest;
import com.wd.api.dto.CrRejectRequest;
import com.wd.api.dto.CrScheduleRequest;
import com.wd.api.dto.ProjectVariationSearchFilter;
import com.wd.api.model.ChangeRequestApprovalHistory;
import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.ProjectVariationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ProjectVariationController {

    @Autowired private ProjectVariationService variationService;
    @Autowired private PortalUserRepository portalUserRepository;

    // ===== Existing /variations endpoints (back-compat; unchanged behaviour) =====

    @GetMapping("/api/projects/{projectId}/variations/search")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<Page<ProjectVariation>> searchProjectVariations(
            @ModelAttribute ProjectVariationSearchFilter filter) {
        return ResponseEntity.ok(variationService.searchProjectVariations(filter));
    }

    @GetMapping("/api/projects/{projectId}/variations")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @Deprecated
    public ResponseEntity<List<ProjectVariation>> getVariations(@PathVariable Long projectId) {
        return ResponseEntity.ok(variationService.getVariationsByProject(projectId));
    }

    @PostMapping("/api/projects/{projectId}/variations")
    @PreAuthorize("hasAnyAuthority('PROJECT_EDIT', 'PROJECT_CREATE')")
    public ResponseEntity<ProjectVariation> createVariation(
            @PathVariable Long projectId,
            @RequestBody ProjectVariation variation,
            Authentication auth) {
        return ResponseEntity.ok(variationService.createVariation(variation, projectId, getCurrentUserId(auth)));
    }

    @PutMapping("/api/projects/{projectId}/variations/{id}")
    @PreAuthorize("hasAnyAuthority('PROJECT_EDIT', 'PROJECT_CREATE')")
    public ResponseEntity<ProjectVariation> updateVariation(
            @PathVariable Long projectId, @PathVariable Long id,
            @RequestBody ProjectVariation variation) {
        return ResponseEntity.ok(variationService.updateVariation(id, variation));
    }

    @DeleteMapping("/api/projects/{projectId}/variations/{id}")
    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    public ResponseEntity<Void> deleteVariation(@PathVariable Long projectId, @PathVariable Long id) {
        variationService.deleteVariation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/projects/{projectId}/variations/{id}/submit")
    @PreAuthorize("hasAnyAuthority('PROJECT_EDIT', 'PROJECT_CREATE')")
    @Deprecated
    public ResponseEntity<ProjectVariation> submitLegacy(
            @PathVariable Long projectId, @PathVariable Long id) {
        return ResponseEntity.ok(variationService.submitForApproval(id));
    }

    @PostMapping("/api/projects/{projectId}/variations/{id}/approve")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    @Deprecated
    public ResponseEntity<ProjectVariation> approveLegacy(
            @PathVariable Long projectId, @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(variationService.approveVariation(id, getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/variations/{id}/reject")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    @Deprecated
    public ResponseEntity<ProjectVariation> rejectLegacy(
            @PathVariable Long projectId, @PathVariable Long id,
            @RequestBody Map<String, String> payload, Authentication auth) {
        String reason = payload.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(variationService.rejectVariation(id, getCurrentUserId(auth), reason));
    }

    // ===== NEW /change-requests endpoints (S4 PR1) =====

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/submit")
    @PreAuthorize("hasAuthority('CR_SUBMIT')")
    public ResponseEntity<ProjectVariation> submitCr(
            @PathVariable Long projectId, @PathVariable Long crId, Authentication auth) {
        return ResponseEntity.ok(variationService.submit(crId, getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/cost")
    @PreAuthorize("hasAuthority('CR_COST')")
    public ResponseEntity<ProjectVariation> costCr(
            @PathVariable Long projectId, @PathVariable Long crId,
            @Valid @RequestBody CrCostRequest body, Authentication auth) {
        return ResponseEntity.ok(variationService.cost(
                crId, body.getCostImpact(), body.getTimeImpactWorkingDays(), getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/send-to-customer")
    @PreAuthorize("hasAuthority('CR_SEND_TO_CUSTOMER')")
    public ResponseEntity<ProjectVariation> sendToCustomer(
            @PathVariable Long projectId, @PathVariable Long crId, Authentication auth) {
        return ResponseEntity.ok(variationService.sendToCustomer(crId, getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/schedule")
    @PreAuthorize("hasAuthority('CR_SCHEDULE')")
    public ResponseEntity<ProjectVariation> scheduleCr(
            @PathVariable Long projectId, @PathVariable Long crId,
            @Valid @RequestBody CrScheduleRequest body, Authentication auth) {
        return ResponseEntity.ok(variationService.schedule(
                crId, body.getAnchorTaskId(), getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/start")
    @PreAuthorize("hasAuthority('CR_START')")
    public ResponseEntity<ProjectVariation> startCr(
            @PathVariable Long projectId, @PathVariable Long crId, Authentication auth) {
        return ResponseEntity.ok(variationService.start(crId, getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/complete")
    @PreAuthorize("hasAuthority('CR_COMPLETE')")
    public ResponseEntity<ProjectVariation> completeCr(
            @PathVariable Long projectId, @PathVariable Long crId, Authentication auth) {
        return ResponseEntity.ok(variationService.complete(crId, getCurrentUserId(auth)));
    }

    @PostMapping("/api/projects/{projectId}/change-requests/{crId}/reject")
    @PreAuthorize("hasAuthority('CR_REJECT')")
    public ResponseEntity<ProjectVariation> rejectCr(
            @PathVariable Long projectId, @PathVariable Long crId,
            @Valid @RequestBody CrRejectRequest body, Authentication auth) {
        return ResponseEntity.ok(variationService.reject(crId, body.getReason(), getCurrentUserId(auth)));
    }

    @GetMapping("/api/projects/{projectId}/change-requests/{crId}/history")
    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    public ResponseEntity<Page<ChangeRequestApprovalHistory>> getCrHistory(
            @PathVariable Long projectId, @PathVariable Long crId, Pageable pageable) {
        return ResponseEntity.ok(variationService.getHistory(crId, pageable));
    }

    // ---- helpers ----

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return portalUserRepository.findByEmail(auth.getName()).map(PortalUser::getId).orElse(null);
    }
}
