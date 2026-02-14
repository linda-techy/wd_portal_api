package com.wd.api.controller;

import com.wd.api.dto.PaymentDtos.*;
import com.wd.api.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 2 Controller: Retention Money & GST Invoice Management
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class RetentionInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(RetentionInvoiceController.class);
    private final PaymentService paymentService;
    private final com.wd.api.service.SubcontractService subcontractService;

    public RetentionInvoiceController(PaymentService paymentService,
            com.wd.api.service.SubcontractService subcontractService) {
        this.paymentService = paymentService;
        this.subcontractService = subcontractService;
    }

    /**
     * Release retention money for a payment
     * POST /api/payments/retention/{workOrderId}/release
     * Note: Path variable changed from paymentId to workOrderId to match logic
     */
    @PostMapping("/retention/{workOrderId}/release")
    public ResponseEntity<?> releaseRetention(
            @PathVariable Long workOrderId,
            @RequestBody ReleaseRetentionRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);

            com.wd.api.model.RetentionRelease release = new com.wd.api.model.RetentionRelease();
            com.wd.api.model.SubcontractWorkOrder wo = new com.wd.api.model.SubcontractWorkOrder();
            wo.setId(workOrderId);
            release.setWorkOrder(wo);
            release.setAmountReleased(request.getReleaseAmount());
            release.setNotes(request.getReleaseReason()); // Map reason to notes
            release.setApprovedById(userId);

            subcontractService.releaseRetention(release);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Retention released successfully",
                    null));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid retention release request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error releasing retention", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to release retention: " + e.getMessage(), null));
        }
    }

    /**
     * Generate GST invoice for a payment
     * POST /api/payments/invoices/generate
     */
    @PostMapping("/invoices/generate")
    public ResponseEntity<?> generateInvoice(
            @RequestBody GenerateInvoiceRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);

            com.wd.api.model.TaxInvoice invoice = paymentService.generateGstInvoice(
                    request.getPaymentId(),
                    request.getPlaceOfSupply(),
                    request.getCustomerGstin(),
                    userId);

            TaxInvoiceResponse response = toTaxInvoiceResponse(invoice);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "GST invoice generated successfully",
                    response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Invalid invoice generation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error generating invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to generate invoice: " + e.getMessage(), null));
        }
    }

    // Helper: Map TaxInvoice entity to DTO
    private TaxInvoiceResponse toTaxInvoiceResponse(com.wd.api.model.TaxInvoice inv) {
        TaxInvoiceResponse r = new TaxInvoiceResponse();
        r.setId(inv.getId());
        r.setInvoiceNumber(inv.getInvoiceNumber());
        r.setPaymentId(inv.getPaymentId());
        r.setCompanyGstin(inv.getCompanyGstin());
        r.setCustomerGstin(inv.getCustomerGstin());
        r.setPlaceOfSupply(inv.getPlaceOfSupply());
        r.setIsInterstate(inv.getIsInterstate());
        r.setTaxableValue(inv.getTaxableValue());
        r.setCgstRate(inv.getCgstRate());
        r.setCgstAmount(inv.getCgstAmount());
        r.setSgstRate(inv.getSgstRate());
        r.setSgstAmount(inv.getSgstAmount());
        r.setIgstRate(inv.getIgstRate());
        r.setIgstAmount(inv.getIgstAmount());
        r.setTotalTaxAmount(inv.getTotalTaxAmount());
        r.setInvoiceTotal(inv.getInvoiceTotal());
        r.setInvoiceDate(inv.getInvoiceDate());
        r.setFinancialYear(inv.getFinancialYear());
        return r;
    }

    // Helper: Extract user ID from authentication
    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Authentication required");
        }
        if (auth.getPrincipal() instanceof com.wd.api.model.PortalUser) {
            return ((com.wd.api.model.PortalUser) auth.getPrincipal()).getId();
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            logger.warn("Could not extract user ID from auth: {}", auth.getName());
            throw new IllegalStateException("Invalid authentication principal");
        }
    }
}
