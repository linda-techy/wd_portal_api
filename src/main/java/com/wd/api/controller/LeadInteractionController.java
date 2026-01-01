package com.wd.api.controller;

import com.wd.api.model.LeadInteraction;
import com.wd.api.service.LeadInteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing lead interactions
 */
@RestController
@RequestMapping("/leads/interactions")
public class LeadInteractionController {

    @Autowired
    private LeadInteractionService interactionService;

    @Autowired
    private com.wd.api.repository.UserRepository userRepository;

    /**
     * Get all interactions for a lead
     */
    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<List<LeadInteraction>> getInteractionsByLead(@PathVariable Long leadId) {
        try {
            List<LeadInteraction> interactions = interactionService.getInteractionsByLeadId(leadId);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get upcoming actions
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<List<LeadInteraction>> getUpcomingActions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<LeadInteraction> interactions = interactionService.getUpcomingActions(startDate, endDate);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get overdue actions
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<List<LeadInteraction>> getOverdueActions() {
        try {
            List<LeadInteraction> interactions = interactionService.getOverdueActions();
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get interaction statistics for a lead
     */
    @GetMapping("/lead/{leadId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<Map<String, Object>> getInteractionStats(@PathVariable Long leadId) {
        try {
            Map<String, Object> stats = interactionService.getInteractionStats(leadId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new interaction
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<?> createInteraction(
            @RequestBody LeadInteraction interaction,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            com.wd.api.model.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LeadInteraction created = interactionService.createInteraction(interaction, user.getId());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to create interaction"));
        }
    }

    /**
     * Log a quick interaction
     */
    @PostMapping("/quick")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<?> logQuickInteraction(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        try {
            Long leadId = Long.parseLong(body.get("leadId").toString());
            String type = (String) body.get("type");
            String notes = (String) body.get("notes");

            String username = authentication.getName();
            com.wd.api.model.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LeadInteraction created = interactionService.logQuickInteraction(leadId, type, notes, user.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Schedule a follow-up
     */
    @PostMapping("/followup")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<?> scheduleFollowUp(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        try {
            Long leadId = Long.parseLong(body.get("leadId").toString());
            String nextAction = (String) body.get("nextAction");
            LocalDateTime nextActionDate = LocalDateTime.parse((String) body.get("nextActionDate"));

            String username = authentication.getName();
            com.wd.api.model.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LeadInteraction created = interactionService.scheduleFollowUp(leadId, nextAction, nextActionDate,
                    user.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update an interaction
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<?> updateInteraction(
            @PathVariable Long id,
            @RequestBody LeadInteraction interaction) {
        try {
            LeadInteraction updated = interactionService.updateInteraction(id, interaction);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Delete an interaction
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> deleteInteraction(@PathVariable Long id) {
        try {
            interactionService.deleteInteraction(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Interaction deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
