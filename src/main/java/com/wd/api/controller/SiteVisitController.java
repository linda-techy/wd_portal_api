package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CheckInRequest;
import com.wd.api.dto.CheckOutRequest;
import com.wd.api.dto.SiteVisitDTO;
import com.wd.api.dto.SiteVisitSearchFilter;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.SiteVisitService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
public class SiteVisitController {

    private static final String KEY_VALUE = "value";
    private static final String KEY_LABEL = "label";

    private final SiteVisitService siteVisitService;
    private final PortalUserRepository portalUserRepository;

    public SiteVisitController(SiteVisitService siteVisitService, PortalUserRepository portalUserRepository) {
        this.siteVisitService = siteVisitService;
        this.portalUserRepository = portalUserRepository;
    }

    /**
     * Search site visits with filters and pagination
     * GET /api/site-visits/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
    public ResponseEntity<Page<SiteVisitDTO>> searchSiteVisits(@ModelAttribute SiteVisitSearchFilter filter) {
        return ResponseEntity.ok(siteVisitService.searchSiteVisits(filter));
    }

    /**
     * Check in to a project site
     * POST /api/site-visits/check-in
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('SITE_VISIT_CREATE')")
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
    @PreAuthorize("hasAuthority('SITE_VISIT_CREATE')")
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
     * Admin force-close: closes a stuck CHECKED_IN visit without GPS validation.
     * Used when a user cannot reach the geofence (lost phone, dead GPS, policy change).
     * Action is auditable: who, when, why are persisted on the visit row.
     * POST /api/site-visits/{id}/force-close
     * Body: { "reason": "string (required)" }
     */
    @PostMapping("/{id}/force-close")
    @PreAuthorize("hasAuthority('SITE_VISIT_FORCE_CLOSE')")
    public ResponseEntity<ApiResponse<SiteVisitDTO>> forceClose(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String reason = body == null ? null : body.get("reason");
            Long adminUserId = getCurrentUserId(auth);
            SiteVisitDTO visit = siteVisitService.forceClose(id, reason, adminUserId);
            return ResponseEntity.ok(ApiResponse.success("Visit force-closed", visit));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get my current active visit
     * GET /api/site-visits/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
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
    @PreAuthorize("hasAuthority('SITE_REPORT_VIEW')")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getAllActiveVisits() {
        return ResponseEntity
                .ok(ApiResponse.success("All active visits retrieved", siteVisitService.getAllActiveVisits()));
    }

    /**
     * Get visits for a specific project
     * GET /api/site-visits/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
    public ResponseEntity<ApiResponse<List<SiteVisitDTO>>> getVisitsByProject(@PathVariable Long projectId) {
        return ResponseEntity
                .ok(ApiResponse.success("Project visits retrieved", siteVisitService.getVisitsByProject(projectId)));
    }

    /**
     * Get today's visits for a project
     * GET /api/site-visits/project/{projectId}/today
     */
    @GetMapping("/project/{projectId}/today")
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
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
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
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
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
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
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
    public ResponseEntity<ApiResponse<SiteVisitDTO>> getVisit(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Site visit details retrieved", siteVisitService.getVisitById(id)));
    }

    /**
     * Cancel a pending visit
     * DELETE /api/site-visits/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SITE_VISIT_CREATE')")
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
    @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getVisitTypes() {
        List<Map<String, String>> types = List.of(
                Map.of(KEY_VALUE, "SITE_ENGINEER", KEY_LABEL, "Site Engineer"),
                Map.of(KEY_VALUE, "PROJECT_MANAGER", KEY_LABEL, "Project Manager"),
                Map.of(KEY_VALUE, "SUPERVISOR", KEY_LABEL, "Supervisor"),
                Map.of(KEY_VALUE, "CONTRACTOR", KEY_LABEL, "Contractor"),
                Map.of(KEY_VALUE, "CLIENT", KEY_LABEL, "Client"),
                Map.of(KEY_VALUE, "GENERAL", KEY_LABEL, "General Visit"));

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
