package com.wd.api.controller;

import com.wd.api.dto.LeadQuotationSearchFilter;
import com.wd.api.model.LeadQuotation;
import com.wd.api.service.LeadQuotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing lead quotations
 */
@RestController
@RequestMapping("/leads/quotations")
public class LeadQuotationController {

    @Autowired
    private LeadQuotationService quotationService;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @GetMapping("/search")
    public ResponseEntity<Page<LeadQuotation>> searchLeadQuotations(@ModelAttribute LeadQuotationSearchFilter filter) {
        return ResponseEntity.ok(quotationService.searchLeadQuotations(filter));
    }

    /**
     * Get quotation by ID with items
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<?> getQuotationById(@PathVariable Long id) {
        try {
            LeadQuotation quotation = quotationService.getQuotationById(id);
            return ResponseEntity.ok(quotation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    /**
     * Get all quotations for a lead
     */
    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER', 'USER')")
    public ResponseEntity<List<LeadQuotation>> getQuotationsByLead(@PathVariable Long leadId) {
        try {
            List<LeadQuotation> quotations = quotationService.getQuotationsByLeadId(leadId);
            return ResponseEntity.ok(quotations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get quotations by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<List<LeadQuotation>> getQuotationsByStatus(@PathVariable String status) {
        try {
            List<LeadQuotation> quotations = quotationService.getQuotationsByStatus(status);
            return ResponseEntity.ok(quotations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new quotation
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> createQuotation(
            @RequestBody LeadQuotation quotation,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            com.wd.api.model.PortalUser user = portalUserRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Portal User not found"));

            LeadQuotation created = quotationService.createQuotation(quotation, user.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update a quotation
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> updateQuotation(
            @PathVariable Long id,
            @RequestBody LeadQuotation quotation) {
        try {
            LeadQuotation updated = quotationService.updateQuotation(id, quotation);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Send quotation to lead
     */
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> sendQuotation(@PathVariable Long id) {
        try {
            LeadQuotation sent = quotationService.sendQuotation(id);
            return ResponseEntity.ok(sent);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Accept quotation
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> acceptQuotation(@PathVariable Long id) {
        try {
            LeadQuotation accepted = quotationService.acceptQuotation(id);
            return ResponseEntity.ok(accepted);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Reject quotation
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> rejectQuotation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            LeadQuotation rejected = quotationService.rejectQuotation(id, reason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Delete quotation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public ResponseEntity<?> deleteQuotation(@PathVariable Long id) {
        try {
            quotationService.deleteQuotation(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Quotation deleted"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
