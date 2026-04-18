package com.wd.api.controller;

import com.wd.api.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for portal staff to manage support tickets raised by customers.
 * All endpoints require authentication; individual methods require specific permissions.
 */
@RestController
@RequestMapping("/api/support/tickets")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SupportTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketService supportTicketService;

    /**
     * List all support tickets with optional filters (paginated).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TICKET_VIEW')")
    public ResponseEntity<?> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long assignedTo) {
        try {
            Map<String, Object> result = supportTicketService.getAllTickets(page, size, status, category, assignedTo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching support tickets", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch support tickets"));
        }
    }

    /**
     * Get full detail of a single support ticket including all replies.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TICKET_VIEW')")
    public ResponseEntity<?> getTicketDetail(@PathVariable Long id) {
        try {
            Map<String, Object> ticket = supportTicketService.getTicketDetail(id);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            logger.error("Error fetching support ticket {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch support ticket"));
        }
    }

    /**
     * Assign a support ticket to a portal staff member.
     * Body: { "assignedTo": <portalUserId> }
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('TICKET_ASSIGN')")
    public ResponseEntity<?> assignTicket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Long assignedTo = Long.valueOf(request.get("assignedTo").toString());
            supportTicketService.assignTicket(id, assignedTo);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ticket assigned successfully",
                    "ticketId", id,
                    "assignedTo", assignedTo
            ));
        } catch (Exception e) {
            logger.error("Error assigning ticket {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to assign ticket"));
        }
    }

    /**
     * Update the status of a support ticket.
     * Body: { "status": "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" }
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('TICKET_MANAGE')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String status = (String) request.get("status");
            supportTicketService.updateStatus(id, status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ticket status updated successfully",
                    "ticketId", id,
                    "status", status
            ));
        } catch (Exception e) {
            logger.error("Error updating status for ticket {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update ticket status"));
        }
    }

    /**
     * Add a staff reply to a support ticket.
     * Body: { "message": "...", "staffName": "...", "attachmentUrl": "..." (optional) }
     */
    @PostMapping("/{id}/replies")
    @PreAuthorize("hasAuthority('TICKET_REPLY')")
    public ResponseEntity<?> addStaffReply(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String message = (String) request.get("message");
            String staffName = (String) request.get("staffName");
            String attachmentUrl = request.get("attachmentUrl") != null
                    ? (String) request.get("attachmentUrl")
                    : null;

            // Resolve the portal user ID from the security context principal name (email)
            // We pass the email as staffUserId fallback; callers should supply staffName explicitly
            Long staffUserId = null;
            if (request.get("staffUserId") != null) {
                staffUserId = Long.valueOf(request.get("staffUserId").toString());
            }
            if (staffName == null || staffName.isBlank()) {
                staffName = authentication.getName();
            }

            Map<String, Object> reply = supportTicketService.addStaffReply(id, staffUserId, staffName,
                    message, attachmentUrl);
            return ResponseEntity.ok(reply);
        } catch (Exception e) {
            logger.error("Error adding reply to ticket {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to add reply to ticket"));
        }
    }
}
