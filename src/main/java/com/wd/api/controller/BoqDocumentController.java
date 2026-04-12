package com.wd.api.controller;

import com.wd.api.dto.*;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.PortalUser;
import com.wd.api.service.BoqDocumentService;
import com.wd.api.service.PaymentStageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST endpoints for BOQ Document lifecycle and payment schedule.
 */
@RestController
@RequestMapping("/api/boq-documents")
@PreAuthorize("isAuthenticated()")
public class BoqDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(BoqDocumentController.class);

    private final BoqDocumentService boqDocumentService;
    private final PaymentStageService paymentStageService;

    public BoqDocumentController(BoqDocumentService boqDocumentService,
                                   PaymentStageService paymentStageService) {
        this.boqDocumentService = boqDocumentService;
        this.paymentStageService = paymentStageService;
    }

    // ---- CRUD ----

    @PostMapping
    @PreAuthorize("hasAuthority('BOQ_CREATE')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> createDocument(
            @Valid @RequestBody CreateBoqDocumentRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqDocument doc = boqDocumentService.createDocument(
                    request.projectId(), request.gstRate(), userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("BOQ document created", BoqDocumentResponse.from(doc)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create BOQ document", e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> getDocument(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK",
                    BoqDocumentResponse.from(boqDocumentService.getDocument(id))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqDocumentResponse>>> getProjectDocuments(
            @PathVariable Long projectId) {
        try {
            List<BoqDocumentResponse> docs = boqDocumentService.getProjectDocuments(projectId)
                    .stream().map(BoqDocumentResponse::from).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", docs));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ documents for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/project/{projectId}/approved")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> getApproved(@PathVariable Long projectId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK",
                    BoqDocumentResponse.from(boqDocumentService.getApprovedDocument(projectId))));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch approved BOQ for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Workflow ----

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> submit(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqDocument doc = boqDocumentService.submitForApproval(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ submitted for approval",
                    BoqDocumentResponse.from(doc)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to submit BOQ document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/approve-internal")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> approveInternally(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqDocument doc = boqDocumentService.approveInternally(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ internally approved",
                    BoqDocumentResponse.from(doc)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to internally approve BOQ document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Records customer approval and generates immutable payment stages.
     * Called when the customer confirms the BOQ via the customer app.
     */
    @PatchMapping("/{id}/customer-approve")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> customerApprove(
            @PathVariable Long id,
            @Valid @RequestBody CustomerApproveBoqRequest request) {
        try {
            Long userId = getCurrentUserId();
            List<BoqDocumentService.StageConfig> stages = request.stages().stream()
                    .map(s -> new BoqDocumentService.StageConfig(s.name(), s.percentage()))
                    .toList();
            BoqDocument doc = boqDocumentService.recordCustomerApproval(id, userId, stages);
            return ResponseEntity.ok(ApiResponse.success("BOQ approved. Payment schedule generated.",
                    BoqDocumentResponse.from(doc)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to record customer approval for BOQ document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqDocumentResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectBoqRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqDocument doc = boqDocumentService.reject(id, userId, request.reason());
            return ResponseEntity.ok(ApiResponse.success("BOQ rejected",
                    BoqDocumentResponse.from(doc)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to reject BOQ document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Payment Stages ----

    @GetMapping("/{id}/payment-stages")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<PaymentStageResponse>>> getPaymentStages(@PathVariable Long id) {
        try {
            List<PaymentStageResponse> stages = paymentStageService.getStagesByDocument(id)
                    .stream().map(PaymentStageResponse::from).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", stages));
        } catch (Exception e) {
            logger.error("Failed to fetch payment stages for document {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/project/{projectId}/payment-stages")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<PaymentStageResponse>>> getProjectPaymentStages(
            @PathVariable Long projectId) {
        try {
            List<PaymentStageResponse> stages = paymentStageService.getStagesByProject(projectId)
                    .stream().map(PaymentStageResponse::from).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", stages));
        } catch (Exception e) {
            logger.error("Failed to fetch payment stages for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("Not authenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof PortalUser user) return user.getId();
        throw new IllegalStateException("Cannot extract user ID");
    }

    // ---- Request DTOs ----

    public record CreateBoqDocumentRequest(
            @NotNull Long projectId,
            BigDecimal gstRate
    ) {}

    public record CustomerApproveBoqRequest(
            @NotNull @Size(min = 1) List<StageConfigDto> stages
    ) {
        public record StageConfigDto(
                @NotBlank String name,
                @NotNull @DecimalMin("0.001") @DecimalMax("1.0") BigDecimal percentage
        ) {}
    }

    public record RejectBoqRequest(
            @NotBlank @Size(max = 1000) String reason
    ) {}
}
