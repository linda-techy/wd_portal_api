package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.BoqInvoiceResponse;
import com.wd.api.model.BoqInvoice;
import com.wd.api.model.CreditNote;
import com.wd.api.model.PortalUser;
import com.wd.api.model.RefundNotice;
import com.wd.api.service.BoqFinanceDashboardService;
import com.wd.api.service.BoqInvoiceService;
import com.wd.api.service.CreditNoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for BOQ Invoices, Credit Notes, Refund Notices, and Finance Dashboard.
 */
@RestController
@RequestMapping("/api/boq-invoices")
@PreAuthorize("isAuthenticated()")
public class BoqInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(BoqInvoiceController.class);

    private final BoqInvoiceService invoiceService;
    private final CreditNoteService creditNoteService;
    private final BoqFinanceDashboardService dashboardService;

    public BoqInvoiceController(BoqInvoiceService invoiceService,
                                  CreditNoteService creditNoteService,
                                  BoqFinanceDashboardService dashboardService) {
        this.invoiceService = invoiceService;
        this.creditNoteService = creditNoteService;
        this.dashboardService = dashboardService;
    }

    // ---- Invoice queries ----

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqInvoiceResponse>> getInvoice(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK",
                    BoqInvoiceResponse.from(invoiceService.getInvoice(id))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch invoice {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqInvoiceResponse>>> getProjectInvoices(
            @PathVariable Long projectId) {
        try {
            List<BoqInvoiceResponse> invoices = invoiceService.getProjectInvoices(projectId)
                    .stream().map(BoqInvoiceResponse::from).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", invoices));
        } catch (Exception e) {
            logger.error("Failed to list invoices for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Raise stage invoice ----

    @PostMapping("/stage/{stageId}/raise")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqInvoiceResponse>> raiseStageInvoice(
            @PathVariable Long stageId,
            @Valid @RequestBody RaiseStageInvoiceRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqInvoice inv = invoiceService.raiseStageInvoice(stageId, request.dueDate(), userId);
            return ResponseEntity.status(201).body(
                    ApiResponse.success("Stage invoice raised", BoqInvoiceResponse.from(inv)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to raise stage invoice for stage {}", stageId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Invoice actions ----

    @PatchMapping("/{id}/send")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqInvoiceResponse>> send(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Invoice sent",
                    BoqInvoiceResponse.from(invoiceService.send(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to send invoice {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqInvoiceResponse>> confirmPayment(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        try {
            BoqInvoice inv = invoiceService.confirmPayment(
                    id, request.paymentReference(), request.paymentMethod(), getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Payment confirmed", BoqInvoiceResponse.from(inv)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to confirm payment for invoice {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/dispute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BoqInvoiceResponse>> dispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeRequest request) {
        try {
            BoqInvoice inv = invoiceService.dispute(id, request.reason(), getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Invoice disputed", BoqInvoiceResponse.from(inv)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to dispute invoice {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Credit Notes ----

    @GetMapping("/project/{projectId}/credit-notes")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCreditNotes(
            @PathVariable Long projectId) {
        try {
            List<Map<String, Object>> notes = creditNoteService.getProjectCreditNotes(projectId)
                    .stream().map(this::creditNoteToMap).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", notes));
        } catch (Exception e) {
            logger.error("Failed to list credit notes for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Refund Notices ----

    @GetMapping("/project/{projectId}/refund-notices")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRefundNotices(
            @PathVariable Long projectId) {
        try {
            List<Map<String, Object>> notices = creditNoteService.getProjectRefundNotices(projectId)
                    .stream().map(this::refundToMap).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", notices));
        } catch (Exception e) {
            logger.error("Failed to list refund notices for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/refund-notices/{refundId}/acknowledge")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acknowledgeRefund(
            @PathVariable Long refundId) {
        try {
            RefundNotice rn = creditNoteService.acknowledgeRefund(refundId, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Refund notice acknowledged", refundToMap(rn)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to acknowledge refund notice {}", refundId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/refund-notices/{refundId}/complete")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        try {
            RefundNotice rn = creditNoteService.completeRefund(
                    refundId, request.paymentReference(), getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.success("Refund completed", refundToMap(rn)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to complete refund notice {}", refundId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Finance Dashboard ----

    @GetMapping("/project/{projectId}/finance-summary")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqFinanceDashboardService.ProjectFinanceSummary>> getFinanceSummary(
            @PathVariable Long projectId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK", dashboardService.getSummary(projectId)));
        } catch (Exception e) {
            logger.error("Failed to get finance summary for project {}", projectId, e);
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

    private Map<String, Object> creditNoteToMap(CreditNote cn) {
        return Map.of(
                "id", cn.getId(),
                "creditNoteNumber", cn.getCreditNoteNumber(),
                "totalCreditInclGst", cn.getTotalCreditInclGst(),
                "remainingBalance", cn.getRemainingBalance(),
                "status", cn.getStatus().name(),
                "issuedAt", cn.getIssuedAt(),
                "changeOrderId", cn.getChangeOrder() != null ? cn.getChangeOrder().getId() : ""
        );
    }

    private Map<String, Object> refundToMap(RefundNotice rn) {
        return Map.of(
                "id", rn.getId(),
                "referenceNumber", rn.getReferenceNumber(),
                "refundAmount", rn.getRefundAmount(),
                "status", rn.getStatus().name(),
                "issuedAt", rn.getIssuedAt(),
                "reason", rn.getReason() != null ? rn.getReason() : ""
        );
    }

    // ---- Request DTOs ----

    public record RaiseStageInvoiceRequest(@NotNull LocalDate dueDate) {}

    public record ConfirmPaymentRequest(
            @NotBlank String paymentReference,
            String paymentMethod
    ) {}

    public record DisputeRequest(
            @NotBlank @jakarta.validation.constraints.Size(max = 1000) String reason
    ) {}
}
