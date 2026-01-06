package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<SiteVisitDTO>> checkIn(@RequestBody CheckInRequest request, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            SiteVisitDTO visit = siteVisitService.checkIn(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Site check-in successful", visit));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check out from a site visit
     * POST /api/site-visits/{id}/check-out
     */
    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<SiteVisitDTO>> checkOut(
            @PathVariable Long id,
            @RequestBody CheckOutRequest request,
            Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            SiteVisitDTO visit = siteVisitService.checkOut(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Site check-out successful", visit));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get my current active visit
     * GET /api/site-visits/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SiteVisitDTO>> getMyActiveVisit(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        SiteVisitDTO visit = siteVisitService.getActiveVisitForUser(userId);
        if (visit == null) {
            return ResponseEntity.ok(ApiResponse.success("No active visit found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Active visit retrieved", visit));
    }

    /**
     * Get all currently active visits (admin view)
     * GET /api/site-visits/all-active
     */
    @GetMapping("/all-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getAllActiveVisits() {
        return ResponseEntity
                .ok(ApiResponse.success("All active visits retrieved", siteVisitService.getAllActiveVisits()));
    }

    /**
     * Get visits for a specific project
     * GET /api/site-visits/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getVisitsByProject(@PathVariable Long projectId) {
        return ResponseEntity
                .ok(ApiResponse.success("Project visits retrieved", siteVisitService.getVisitsByProject(projectId)));
    }

    /**
     * Get today's visits for a project
     * GET /api/site-visits/project/{projectId}/today
     */
    @GetMapping("/project/{projectId}/today")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getTodaysVisits(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success("Today's project visits retrieved",
                siteVisitService.getTodaysVisitsForProject(projectId)));
    }

    /**
     * Get visits by project and date range
     * GET
     * /api/site-visits/project/{projectId}/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/project/{projectId}/range")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getVisitsByProjectAndDateRange(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success("Visits retrieved for date range",
                siteVisitService.getVisitsByProjectAndDateRange(projectId, startDate, endDate)));
    }

    /**
     * Get my visit history
     * GET /api/site-visits/my-history?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getMyVisitHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.success("My visit history retrieved",
                siteVisitService.getVisitsByUserAndDateRange(userId, startDate, endDate)));
    }

    /**
     * Get a specific visit by ID
     * GET /api/site-visits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteVisitDTO>> getVisit(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Site visit details retrieved", siteVisitService.getVisitById(id)));
    }

    /**
     * Cancel a pending visit
     * DELETE /api/site-visits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelVisit(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getCurrentUserId(auth);
            siteVisitService.cancelVisit(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Visit cancelled successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get visit types for dropdown
     * GET /api/site-visits/types
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getVisitTypes() {
        List<Map<String, String>> types = List.of(
                Map.of("value", "SITE_ENGINEER", "label", "Site Engineer"),
                Map.of("value", "PROJECT_MANAGER", "label", "Project Manager"),
                Map.of("value", "SUPERVISOR", "label", "Supervisor"),
                Map.of("value", "CONTRACTOR", "label", "Contractor"),
                Map.of("value", "CLIENT", "label", "Client"),
                Map.of("value", "GENERAL", "label", "General Visit"));

        return ResponseEntity.ok(ApiResponse.success("Visit types retrieved", types));
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
