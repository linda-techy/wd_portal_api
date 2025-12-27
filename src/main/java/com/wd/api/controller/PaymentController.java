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

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Create a new design package payment agreement
     */
    @PostMapping("/design")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> createDesignPayment(
            @RequestBody CreateDesignPaymentRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            DesignPaymentResponse response = paymentService.createDesignPayment(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Design payment created successfully", response));
        } catch (IllegalStateException e) {
            logger.warn("Design payment creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error creating design payment", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error creating design payment: " + e.getMessage(), null));
        }
    }

    /**
     * Get design payment details for a project
     */
    @GetMapping("/design/project/{projectId}")
    public ResponseEntity<?> getDesignPaymentByProject(@PathVariable Long projectId) {
        try {
            DesignPaymentResponse response = paymentService.getDesignPaymentByProjectId(projectId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Design payment retrieved successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error getting design payment for project: {}", projectId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving design payment", null));
        }
    }

    /**
     * Get all payments (for dashboard view)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                    size);
            var payments = paymentService.getAllPayments(search, pageable);
            return ResponseEntity.ok(new ApiResponse<>(true, "Payments retrieved successfully", payments));
        } catch (Exception e) {
            logger.error("Error getting all payments", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving payments", null));
        }
    }

    /**
     * Get pending payments (for dashboard view)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                    size);
            var payments = paymentService.getPendingPayments(search, pageable);
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending payments retrieved successfully", payments));
        } catch (Exception e) {
            logger.error("Error getting pending payments", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving pending payments", null));
        }
    }

    /**
     * Record a payment transaction against a schedule
     */
    @PostMapping("/schedule/{scheduleId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> recordTransaction(
            @PathVariable Long scheduleId,
            @RequestBody RecordTransactionRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            TransactionResponse response = paymentService.recordTransaction(scheduleId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Transaction recorded successfully", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Transaction recording failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error recording transaction for schedule: {}", scheduleId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error recording transaction: " + e.getMessage(), null));
        }
    }

    /**
     * Get transaction history (Financial Ledger)
     */
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                    size,
                    org.springframework.data.domain.Sort.by("paymentDate").descending());

            var history = paymentService.getTransactionHistory(search, method, status, startDate, endDate, pageable);
            return ResponseEntity.ok(new ApiResponse<>(true, "Transaction history retrieved successfully", history));
        } catch (Exception e) {
            logger.error("Error getting transaction history", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving transaction history", null));
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        try {
            if (auth.getPrincipal() instanceof com.wd.api.model.User) {
                return ((com.wd.api.model.User) auth.getPrincipal()).getId();
            }
            // Fallback to name if it's a number (for legacy or specific auth types)
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            logger.warn("Could not extract user ID from auth: {}", auth.getName());
            return null;
        }
    }
}
