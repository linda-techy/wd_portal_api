package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.StagePaymentDtos.*;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.PortalUser;
import com.wd.api.service.StagePaymentCertificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for stage payment certification and payment recording.
 *
 * Base path: /api/projects/{projectId}/stages
 */
@RestController
@RequestMapping("/api/projects/{projectId}/stages")
@PreAuthorize("isAuthenticated()")
public class StagePaymentController {

    private final StagePaymentCertificationService stageService;

    public StagePaymentController(StagePaymentCertificationService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAGE_VIEW')")
    public ResponseEntity<ApiResponse<List<StageTimelineSummary>>> list(
            @PathVariable Long projectId) {
        List<PaymentStage> stages = stageService.getProjectStages(projectId);
        List<StageTimelineSummary> summaries = stages.stream()
                .map(StageTimelineSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Stages retrieved", summaries));
    }

    @GetMapping("/{stageId}")
    @PreAuthorize("hasAuthority('STAGE_VIEW')")
    public ResponseEntity<ApiResponse<StageCertificationResponse>> get(
            @PathVariable Long projectId, @PathVariable Long stageId) {
        PaymentStage stage = stageService.getStage(stageId);
        return ResponseEntity.ok(ApiResponse.success("Stage retrieved",
                StageCertificationResponse.from(stage)));
    }

    @PostMapping("/{stageId}/certify")
    @PreAuthorize("hasAuthority('STAGE_CERTIFY')")
    public ResponseEntity<ApiResponse<StageCertificationResponse>> certify(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @Valid @RequestBody CertifyStageRequest req) {
        PaymentStage stage = stageService.certify(stageId, req, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Stage certified successfully",
                StageCertificationResponse.from(stage)));
    }

    @PostMapping("/{stageId}/invoice")
    @PreAuthorize("hasAuthority('STAGE_INVOICE')")
    public ResponseEntity<ApiResponse<StageCertificationResponse>> attachInvoice(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @Valid @RequestBody InvoiceStageRequest req) {
        PaymentStage stage = stageService.attachInvoice(stageId, req, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Invoice attached to stage",
                StageCertificationResponse.from(stage)));
    }

    @PostMapping("/{stageId}/payment")
    @PreAuthorize("hasAuthority('STAGE_PAYMENT_RECORD')")
    public ResponseEntity<ApiResponse<StageCertificationResponse>> recordPayment(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @Valid @RequestBody RecordStagePaymentRequest req) {
        PaymentStage stage = stageService.recordPayment(stageId, req, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Payment recorded",
                StageCertificationResponse.from(stage)));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PortalUser user) return user.getId();
        throw new IllegalStateException("Unable to extract user from authentication context");
    }
}
