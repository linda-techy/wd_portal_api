package com.wd.api.controller;

import com.wd.api.model.Lead;
import com.wd.api.model.PartnershipUser;
import com.wd.api.repository.LeadRepository;
import com.wd.api.service.JwtService;
import com.wd.api.service.PartnershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin controller for managing partnership/referral partners.
 * All endpoints require authenticated portal staff (anyRequest().authenticated() in SecurityConfig).
 * Uses /api/admin/partnerships/** prefix — distinct from partner-facing /api/partnerships/**.
 */
@RestController
@RequestMapping("/api/admin/partnerships")
public class AdminPartnershipController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPartnershipController.class);

    @Autowired
    private PartnershipService partnershipService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LeadRepository leadRepository;

    /**
     * List partners — paginated, searchable, filterable by status and type.
     * GET /api/admin/partnerships?status=pending&partnershipType=architect&search=john&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<?> listPartners(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String partnershipType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PartnershipUser> partnersPage = partnershipService.searchPartners(
                    status, partnershipType, search, page, size);

            List<Map<String, Object>> summaries = partnersPage.getContent().stream()
                    .map(partnershipService::toAdminSummary)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("content", summaries);
            response.put("totalElements", partnersPage.getTotalElements());
            response.put("totalPages", partnersPage.getTotalPages());
            response.put("currentPage", partnersPage.getNumber());
            response.put("pageSize", partnersPage.getSize());
            response.put("hasNext", partnersPage.hasNext());
            response.put("hasPrevious", partnersPage.hasPrevious());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to list partners: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve partners"));
        }
    }

    /**
     * Status summary counts — badge counts for tabs in the UI.
     * GET /api/admin/partnerships/counts
     */
    @GetMapping("/counts")
    public ResponseEntity<?> getStatusCounts() {
        try {
            return ResponseEntity.ok(partnershipService.getPartnerStatusCounts());
        } catch (Exception e) {
            logger.error("Failed to get partner counts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve counts"));
        }
    }

    /**
     * Get full partner detail including stats.
     * GET /api/admin/partnerships/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartner(@PathVariable Long id) {
        try {
            Map<String, Object> detail = partnershipService.getPartnerAdminDetail(id);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            logger.warn("Partner not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Partner not found"));
        }
    }

    /**
     * Update partner status — approve, reject, suspend, or reactivate.
     * PUT /api/admin/partnerships/{id}/status
     * Body: { "status": "approved", "note": "optional reason" }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
            }
            List<String> validStatuses = List.of("pending", "approved", "active", "rejected", "suspended");
            if (!validStatuses.contains(newStatus)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Must be one of: " + validStatuses));
            }

            String adminEmail = extractAdminEmail(authHeader);
            partnershipService.updatePartnerStatus(id, newStatus, adminEmail);

            logger.info("Admin {} updated partner {} status to {}", adminEmail, id, newStatus);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Partner status updated to " + newStatus,
                    "partnerId", id,
                    "newStatus", newStatus
            ));
        } catch (RuntimeException e) {
            logger.error("Failed to update partner status {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get referrals (leads) submitted by a partner — for the admin detail view.
     * GET /api/admin/partnerships/{id}/referrals
     */
    @GetMapping("/{id}/referrals")
    public ResponseEntity<?> getPartnerReferrals(@PathVariable Long id) {
        try {
            List<Map<String, Object>> referrals = partnershipService.getReferralSummaries(id);
            return ResponseEntity.ok(referrals);
        } catch (RuntimeException e) {
            logger.error("Failed to get referrals for partner {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Suspend a partner account (soft delete — keeps all data).
     * DELETE /api/admin/partnerships/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> suspendPartner(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String adminEmail = extractAdminEmail(authHeader);
            partnershipService.suspendPartner(id, adminEmail);
            logger.info("Admin {} suspended partner {}", adminEmail, id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Partner account suspended"
            ));
        } catch (RuntimeException e) {
            logger.error("Failed to suspend partner {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resolve the referring partner for a given lead.
     * Parses the lead's notes field to find the partnerId.
     * GET /api/admin/partnerships/by-lead/{leadId}
     * Returns 404 if lead has no partner attribution.
     */
    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<?> getPartnerByLead(@PathVariable Long leadId) {
        try {
            Lead lead = leadRepository.findById(leadId).orElse(null);
            if (lead == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Lead not found"));
            }

            String notes = lead.getNotes();
            String source = lead.getLeadSource();
            if (notes == null || (!("referral_architect".equals(source)) && !("referral_client".equals(source)))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No partner attribution for this lead"));
            }

            // Parse partnerId from notes
            Long partnerId = extractPartnerIdFromNotes(notes, source);
            if (partnerId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Partner ID not found in lead notes"));
            }

            PartnershipUser partner = partnershipService.getPartnerById(partnerId);
            Map<String, Object> result = new HashMap<>();
            result.put("partnerId", partner.getId());
            result.put("fullName", partner.getFullName());
            result.put("email", partner.getEmail());
            result.put("phone", partner.getPhone());
            result.put("partnershipType", partner.getPartnershipType());
            result.put("firmName", partner.getFirmName() != null ? partner.getFirmName() : partner.getCompanyName());
            result.put("status", partner.getStatus());
            result.put("leadSource", source);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.error("Failed to get partner for lead {}: {}", leadId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Partner not found"));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Parses partner ID from the notes field based on source type. */
    private Long extractPartnerIdFromNotes(String notes, String source) {
        if (notes == null) return null;
        try {
            if ("referral_architect".equals(source)) {
                // Pattern: "(ID: 123)"
                Matcher m = Pattern.compile("\\(ID:\\s*(\\d+)\\)").matcher(notes);
                if (m.find()) return Long.parseLong(m.group(1));
            } else if ("referral_client".equals(source)) {
                // Pattern: "Partner ID: 123"
                Matcher m = Pattern.compile("Partner ID:\\s*(\\d+)").matcher(notes);
                if (m.find()) return Long.parseLong(m.group(1));
            }
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private String extractAdminEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "system";
        }
        try {
            String token = authHeader.substring(7);
            return jwtService.extractActualSubject(token);
        } catch (Exception e) {
            return "system";
        }
    }
}
