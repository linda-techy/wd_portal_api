package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.LeadQuotationSearchFilter;
import com.wd.api.dto.quotation.AddItemFromCatalogRequest;
import com.wd.api.dto.quotation.AssumptionRequest;
import com.wd.api.dto.quotation.AssumptionResponse;
import com.wd.api.dto.quotation.ExclusionRequest;
import com.wd.api.dto.quotation.ExclusionResponse;
import com.wd.api.dto.quotation.InclusionRequest;
import com.wd.api.dto.quotation.InclusionResponse;
import com.wd.api.dto.quotation.LeadQuotationDetailResponse;
import com.wd.api.dto.quotation.PaymentMilestoneRequest;
import com.wd.api.dto.quotation.PaymentMilestoneResponse;
import com.wd.api.dto.quotation.PromoteItemToCatalogRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.model.LeadQuotation;
import com.wd.api.model.LeadQuotationItem;
import com.wd.api.service.LeadQuotationService;
import com.wd.api.service.QuotationCatalogPromotionService;
import com.wd.api.service.QuotationSubResourceService;
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

    @Autowired
    private QuotationSubResourceService subResourceService;

    @Autowired
    private com.wd.api.service.PublicQuotationService publicQuotationService;

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
     * The 16-row Walldot standard scope library — used by the Flutter
     * "Load Walldot scopes" button on the SQFT_RATE add screen to seed a
     * fresh quotation with the standard descriptive content. Static
     * payload (no auth needed beyond LEAD_VIEW); the Flutter side caches
     * it for the session.
     */
    @GetMapping("/standard-scopes")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<java.util.List<com.wd.api.dto.quotation.StandardScopeTemplate>> standardScopes() {
        return ResponseEntity.ok(
                com.wd.api.dto.quotation.StandardScopeTemplate.WALLDOT_DEFAULTS);
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

    // ── V76 sub-resources: inclusions / exclusions / assumptions / milestones ──
    //
    // Each group is a thin controller wrapper around QuotationSubResourceService.
    // Status mapping:
    //   404  — quotation or sub-resource not found
    //   422  — domain rule violation (e.g. duplicate milestone number)
    //   400  — validation failure (caught by Spring's MethodArgumentNotValidException
    //          handler globally; we don't repeat per-method)
    //   500  — unexpected; logged with quotationId for forensics

    @GetMapping("/{quotationId}/inclusions")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> listInclusions(@PathVariable Long quotationId) {
        try {
            List<InclusionResponse> body = subResourceService.listInclusions(quotationId).stream()
                    .map(InclusionResponse::from)
                    .toList();
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{quotationId}/inclusions")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> createInclusion(
            @PathVariable Long quotationId, @Valid @RequestBody InclusionRequest req) {
        try {
            InclusionResponse body = InclusionResponse.from(
                    subResourceService.createInclusion(quotationId, req));
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create inclusion on quotation {}", quotationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal error creating inclusion"));
        }
    }

    @PutMapping("/{quotationId}/inclusions/{inclusionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> updateInclusion(
            @PathVariable Long quotationId, @PathVariable Long inclusionId,
            @Valid @RequestBody InclusionRequest req) {
        try {
            return ResponseEntity.ok(InclusionResponse.from(
                    subResourceService.updateInclusion(quotationId, inclusionId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{quotationId}/inclusions/{inclusionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> deleteInclusion(
            @PathVariable Long quotationId, @PathVariable Long inclusionId) {
        try {
            subResourceService.deleteInclusion(quotationId, inclusionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{quotationId}/exclusions")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> listExclusions(@PathVariable Long quotationId) {
        try {
            return ResponseEntity.ok(subResourceService.listExclusions(quotationId).stream()
                    .map(ExclusionResponse::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{quotationId}/exclusions")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> createExclusion(
            @PathVariable Long quotationId, @Valid @RequestBody ExclusionRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ExclusionResponse.from(
                    subResourceService.createExclusion(quotationId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create exclusion on quotation {}", quotationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal error creating exclusion"));
        }
    }

    @PutMapping("/{quotationId}/exclusions/{exclusionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> updateExclusion(
            @PathVariable Long quotationId, @PathVariable Long exclusionId,
            @Valid @RequestBody ExclusionRequest req) {
        try {
            return ResponseEntity.ok(ExclusionResponse.from(
                    subResourceService.updateExclusion(quotationId, exclusionId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{quotationId}/exclusions/{exclusionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> deleteExclusion(
            @PathVariable Long quotationId, @PathVariable Long exclusionId) {
        try {
            subResourceService.deleteExclusion(quotationId, exclusionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{quotationId}/assumptions")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> listAssumptions(@PathVariable Long quotationId) {
        try {
            return ResponseEntity.ok(subResourceService.listAssumptions(quotationId).stream()
                    .map(AssumptionResponse::from).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{quotationId}/assumptions")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> createAssumption(
            @PathVariable Long quotationId, @Valid @RequestBody AssumptionRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(AssumptionResponse.from(
                    subResourceService.createAssumption(quotationId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create assumption on quotation {}", quotationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal error creating assumption"));
        }
    }

    @PutMapping("/{quotationId}/assumptions/{assumptionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> updateAssumption(
            @PathVariable Long quotationId, @PathVariable Long assumptionId,
            @Valid @RequestBody AssumptionRequest req) {
        try {
            return ResponseEntity.ok(AssumptionResponse.from(
                    subResourceService.updateAssumption(quotationId, assumptionId, req)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{quotationId}/assumptions/{assumptionId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> deleteAssumption(
            @PathVariable Long quotationId, @PathVariable Long assumptionId) {
        try {
            subResourceService.deleteAssumption(quotationId, assumptionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{quotationId}/payment-milestones")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> listPaymentMilestones(@PathVariable Long quotationId) {
        try {
            List<PaymentMilestoneResponse> rows = subResourceService.listMilestones(quotationId).stream()
                    .map(PaymentMilestoneResponse::from).toList();
            // Body wraps the list with a running percentage total so the
            // Flutter editor can render the "85% allocated, 15% remaining"
            // hint without a second round-trip.
            return ResponseEntity.ok(Map.of(
                    "milestones", rows,
                    "totalPercentage", subResourceService.totalMilestonePercentage(quotationId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{quotationId}/payment-milestones")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> createPaymentMilestone(
            @PathVariable Long quotationId, @Valid @RequestBody PaymentMilestoneRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(PaymentMilestoneResponse.from(
                    subResourceService.createMilestone(quotationId, req)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create milestone on quotation {}", quotationId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal error creating milestone"));
        }
    }

    @PutMapping("/{quotationId}/payment-milestones/{milestoneId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> updatePaymentMilestone(
            @PathVariable Long quotationId, @PathVariable Long milestoneId,
            @Valid @RequestBody PaymentMilestoneRequest req) {
        try {
            return ResponseEntity.ok(PaymentMilestoneResponse.from(
                    subResourceService.updateMilestone(quotationId, milestoneId, req)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{quotationId}/payment-milestones/{milestoneId}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<?> deletePaymentMilestone(
            @PathVariable Long quotationId, @PathVariable Long milestoneId) {
        try {
            subResourceService.deleteMilestone(quotationId, milestoneId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── V77 share-link management ─────────────────────────────────────────

    /**
     * Generate a fresh public_view_token (or rotate an existing one). Used
     * both at "Send" time and on staff-triggered rotation if a customer
     * reports a leaked link. Returns the new token so the Flutter UI can
     * compose a fresh share URL.
     */
    @PostMapping("/{id}/regenerate-token")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT', 'LEAD_CREATE')")
    public ResponseEntity<?> regeneratePublicToken(@PathVariable Long id) {
        try {
            java.util.UUID token = publicQuotationService.regenerateToken(id);
            return ResponseEntity.ok(Map.of("publicViewToken", token.toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Total customer-side hits on the public token-gated endpoint for this
     * quotation. Powers the "viewed N times" badge on the lead screen.
     */
    @GetMapping("/{id}/view-count")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<?> getViewCount(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("viewCount", publicQuotationService.viewCount(id)));
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
