package com.wd.api.controller;

import com.wd.api.dto.CheckInRequest;
import com.wd.api.dto.CheckOutRequest;
import com.wd.api.dto.SiteVisitDTO;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.SiteVisitService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Site Visit Check-In/Check-Out functionality
 */
@RestController
@RequestMapping("/api/site-visits")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;
    private final PortalUserRepository portalUserRepository;

    public SiteVisitController(SiteVisitService siteVisitService, PortalUserRepository portalUserRepository) {
        this.siteVisitService = siteVisitService;
        this.portalUserRepository = portalUserRepository;
    }

    /**
     * Check in to a project site
     * POST /api/site-visits/check-in
     */
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest request, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            SiteVisitDTO visit = siteVisitService.checkIn(request, userId);
            return ResponseEntity.ok(visit);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check out from a site visit
     * POST /api/site-visits/{id}/check-out
     */
    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(
            @PathVariable Long id,
            @RequestBody CheckOutRequest request,
            Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            SiteVisitDTO visit = siteVisitService.checkOut(id, request, userId);
            return ResponseEntity.ok(visit);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get my current active visit
     * GET /api/site-visits/active
     */
    @GetMapping("/active")
    public ResponseEntity<SiteVisitDTO> getMyActiveVisit(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        SiteVisitDTO visit = siteVisitService.getActiveVisitForUser(userId);
        if (visit == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(visit);
    }

    /**
     * Get all currently active visits (admin view)
     * GET /api/site-visits/all-active
     */
    @GetMapping("/all-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SiteVisitDTO>> getAllActiveVisits() {
        return ResponseEntity.ok(siteVisitService.getAllActiveVisits());
    }

    /**
     * Get visits for a specific project
     * GET /api/site-visits/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SiteVisitDTO>> getVisitsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(siteVisitService.getVisitsByProject(projectId));
    }

    /**
     * Get today's visits for a project
     * GET /api/site-visits/project/{projectId}/today
     */
    @GetMapping("/project/{projectId}/today")
    public ResponseEntity<List<SiteVisitDTO>> getTodaysVisits(@PathVariable Long projectId) {
        return ResponseEntity.ok(siteVisitService.getTodaysVisitsForProject(projectId));
    }

    /**
     * Get visits by project and date range
     * GET
     * /api/site-visits/project/{projectId}/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/project/{projectId}/range")
    public ResponseEntity<List<SiteVisitDTO>> getVisitsByProjectAndDateRange(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(siteVisitService.getVisitsByProjectAndDateRange(projectId, startDate, endDate));
    }

    /**
     * Get my visit history
     * GET /api/site-visits/my-history?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/my-history")
    public ResponseEntity<List<SiteVisitDTO>> getMyVisitHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(siteVisitService.getVisitsByUserAndDateRange(userId, startDate, endDate));
    }

    /**
     * Get a specific visit by ID
     * GET /api/site-visits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SiteVisitDTO> getVisit(@PathVariable Long id) {
        return ResponseEntity.ok(siteVisitService.getVisitById(id));
    }

    /**
     * Cancel a pending visit
     * DELETE /api/site-visits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelVisit(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            siteVisitService.cancelVisit(id, userId);
            return ResponseEntity.ok(Map.of("message", "Visit cancelled"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get visit types for dropdown
     * GET /api/site-visits/types
     */
    @GetMapping("/types")
    public ResponseEntity<?> getVisitTypes() {
        return ResponseEntity.ok(List.of(
                Map.of("value", "SITE_ENGINEER", "label", "Site Engineer"),
                Map.of("value", "PROJECT_MANAGER", "label", "Project Manager"),
                Map.of("value", "SUPERVISOR", "label", "Supervisor"),
                Map.of("value", "CONTRACTOR", "label", "Contractor"),
                Map.of("value", "CLIENT", "label", "Client"),
                Map.of("value", "GENERAL", "label", "General Visit")));
    }

    /**
     * Helper to get current user ID from security context
     */
    private Long getCurrentUserId(Authentication auth) {
        String email = auth.getName();
        PortalUser user = portalUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
