package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.LeadQuotationSearchFilter;
import com.wd.api.dto.quotation.AddItemFromCatalogRequest;
import com.wd.api.dto.quotation.LeadQuotationDetailResponse;
import com.wd.api.dto.quotation.PromoteItemToCatalogRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.service.LeadQuotationService;
import com.wd.api.service.QuotationCatalogPromotionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private static final Logger logger = LoggerFactory.getLogger(LeadQuotationController.class);

    @Autowired
    private LeadQuotationService quotationService;

    @Autowired
    private com.wd.api.repository.PortalUserRepository portalUserRepository;

    @Autowired
    private QuotationCatalogPromotionService catalogPromotionService;

    @GetMapping("/search")
    public ResponseEntity<Page<LeadQuotation>> searchLeadQuotations(@ModelAttribute LeadQuotationSearchFilter filter) {
        return ResponseEntity.ok(quotationService.searchLeadQuotations(filter));
    }

    /**
     * Get quotation by ID with items
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> getQuotationById(@PathVariable Long id) {
        try {
            LeadQuotation quotation = quotationService.getQuotationById(id);
            return ResponseEntity.ok(LeadQuotationDetailResponse.from(quotation));
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
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
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
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
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
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
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
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
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
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
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
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
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
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
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
    @PreAuthorize("hasAuthority('LEAD_DELETE')")
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

    /**
     * Add a new line item to a quotation, sourced from the master catalog.
     */
    @PostMapping("/{quotationId}/items/from-catalog")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> addItemFromCatalog(
            @PathVariable Long quotationId,
            @Valid @RequestBody AddItemFromCatalogRequest request) {
        try {
            LeadQuotationItem item = quotationService.addItemFromCatalog(quotationId, request);
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("id", item.getId());
            body.put("itemNumber", item.getItemNumber());
            body.put("description", item.getDescription());
            body.put("quantity", item.getQuantity());
            body.put("unitPrice", item.getUnitPrice());
            body.put("totalPrice", item.getTotalPrice());
            body.put("catalogItemId", item.getCatalogItem() != null ? item.getCatalogItem().getId() : null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Item added from catalog", body));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add catalog item to quotation {}", quotationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while adding the catalog item"));
        }
    }

    /**
     * Promote an ad-hoc quotation line item into the master catalog.
     */
    @PostMapping("/items/{itemId}/promote-to-catalog")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'QUOTATION_CATALOG_MANAGE')")
    public ResponseEntity<?> promoteItemToCatalog(
            @PathVariable Long itemId,
            @Valid @RequestBody PromoteItemToCatalogRequest request) {
        try {
            QuotationCatalogItemDto dto = catalogPromotionService.promoteAdHocItem(itemId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Item promoted to catalog", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to promote item {} to catalog", itemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while promoting the item"));
        }
    }

    /**
     * Restore a soft-deleted quotation. Powers the Flutter Undo snackbar
     * after a delete. 422 when the row isn't currently tombstoned (already
     * restored, never deleted, or unknown id).
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('LEAD_DELETE')")
    public ResponseEntity<?> restoreQuotation(@PathVariable Long id) {
        try {
            LeadQuotation restored = quotationService.restoreQuotation(id);
            return ResponseEntity.ok(restored);
        } catch (RuntimeException e) {
            logger.error("Failed to restore quotation {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Pipeline summary — feeds the Flutter list-screen hero card.
     * Open count + value (DRAFT/SENT/VIEWED), accepted count + value over a
     * 90-day window, win rate, and average close days.
     */
    @GetMapping("/pipeline-summary")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<com.wd.api.dto.quotation.PipelineSummaryResponse> getPipelineSummary() {
        return ResponseEntity.ok(quotationService.getPipelineSummary());
    }

    /**
     * Duplicate an existing quotation as a fresh DRAFT — copies header,
     * pricing knobs, and items. The most-requested missing CRM action.
     */
    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
    public ResponseEntity<?> duplicateQuotation(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            com.wd.api.model.PortalUser user = portalUserRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Portal User not found"));
            LeadQuotation copy = quotationService.duplicateQuotation(id, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(copy);
        } catch (RuntimeException e) {
            logger.error("Failed to duplicate quotation {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Download quotation PDF for client presentation
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> downloadQuotationPdf(@PathVariable Long id) {
        try {
            // First verify quotation exists
            LeadQuotation quotation = quotationService.getQuotationById(id);
            
            // Generate PDF
            byte[] pdf = quotationService.generateQuotationPdf(id);
            
            // Generate filename
            String filename = "Quotation_" + (quotation.getQuotationNumber() != null ? 
                quotation.getQuotationNumber().replace("/", "_") : "ID_" + id) + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            headers.setContentLength(pdf.length);
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Error generating PDF for quotation {}: {}", id, e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quotation not found with id: " + id));
            }
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error generating PDF for quotation {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Error generating PDF: " + e.getMessage()));
        }
    }
}
