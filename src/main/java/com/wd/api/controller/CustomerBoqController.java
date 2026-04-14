package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CustomerBoqSummary;
import com.wd.api.dto.CustomerPaymentStageView;
import com.wd.api.model.BoqDocument;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.PaymentStageRepository;
import com.wd.api.security.ProjectAccessGuard;
import com.wd.api.service.BoqDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Customer-facing BOQ endpoints.
 * Accessible only to authenticated CustomerUser principals (ROLE_CUSTOMER).
 * Returns lean DTOs — no unit rates, no execution data, no internal user IDs.
 */
@RestController
@RequestMapping("/api/customer/boq")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerBoqController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerBoqController.class);

    private final BoqDocumentRepository boqDocumentRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final CustomerUserRepository customerUserRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final BoqDocumentService boqDocumentService;

    public CustomerBoqController(BoqDocumentRepository boqDocumentRepository,
                                  PaymentStageRepository paymentStageRepository,
                                  CustomerUserRepository customerUserRepository,
                                  ProjectAccessGuard projectAccessGuard,
                                  BoqDocumentService boqDocumentService) {
        this.boqDocumentRepository = boqDocumentRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.customerUserRepository = customerUserRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.boqDocumentService = boqDocumentService;
    }

    /**
     * Returns the BOQ summary for a project.
     * Shows the approved document if one exists; falls back to the latest
     * non-rejected draft/pending document so the customer sees progress.
     */
    @GetMapping("/project/{projectId}/summary")
    public ResponseEntity<ApiResponse<CustomerBoqSummary>> getSummary(@PathVariable Long projectId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            projectAccessGuard.verifyCustomerAccess(customerUserId, projectId);

            // Prefer approved document; fall back to latest active document
            Optional<BoqDocument> docOpt = boqDocumentRepository.findApprovedByProjectId(projectId);
            if (docOpt.isEmpty()) {
                List<BoqDocument> active = boqDocumentRepository.findActiveByProjectId(projectId);
                docOpt = active.stream()
                        .filter(d -> !d.isRejected())
                        .max(java.util.Comparator.comparing(BoqDocument::getRevisionNumber));
            }
            if (docOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No BOQ available yet", null));
            }

            BoqDocument doc = docOpt.get();
            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByBoqDocumentIdOrderByStageNumberAsc(doc.getId())
                    .stream().map(CustomerPaymentStageView::from).toList();

            return ResponseEntity.ok(ApiResponse.success("OK", CustomerBoqSummary.from(doc, stages)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ summary for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Returns the payment stages for a project's approved BOQ.
     * Returns empty list if no approved BOQ exists yet.
     */
    @GetMapping("/project/{projectId}/payment-stages")
    public ResponseEntity<ApiResponse<List<CustomerPaymentStageView>>> getPaymentStages(
            @PathVariable Long projectId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            projectAccessGuard.verifyCustomerAccess(customerUserId, projectId);

            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByProjectIdOrderByStageNumberAsc(projectId)
                    .stream().map(CustomerPaymentStageView::from).toList();

            return ResponseEntity.ok(ApiResponse.success("OK", stages));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch payment stages for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Records the customer's digital acknowledgement of a BOQ document.
     * Idempotent — safe to call multiple times.
     */
    @PatchMapping("/documents/{documentId}/acknowledge")
    public ResponseEntity<ApiResponse<CustomerBoqSummary>> acknowledge(@PathVariable Long documentId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            BoqDocument doc = boqDocumentService.acknowledgeDocument(documentId, customerUserId);
            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByBoqDocumentIdOrderByStageNumberAsc(doc.getId())
                    .stream().map(CustomerPaymentStageView::from).toList();
            return ResponseEntity.ok(ApiResponse.success("BOQ acknowledged", CustomerBoqSummary.from(doc, stages)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to acknowledge BOQ document {}", documentId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Resolves the authenticated CustomerUser from the security context.
     * The JwtAuthenticationFilter stores the customer's email as principal for CUSTOMER tokens.
     */
    private Long getCurrentCustomerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        String email = (String) auth.getPrincipal();
        return customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"))
                .getId();
    }
}
