package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.VariationOrderDtos.*;
import com.wd.api.model.ChangeOrder;
import com.wd.api.model.ChangeOrderApprovalHistory;
import com.wd.api.model.ChangeOrderPaymentSchedule;
import com.wd.api.model.PortalUser;
import com.wd.api.model.enums.ApprovalLevel;
import com.wd.api.service.VariationOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for Variation Order (VO) management.
 *
 * Base path: /api/projects/{projectId}/variation-orders
 *
 * Kept separate from the legacy /api/projects/{projectId}/variations endpoint
 * (ProjectVariationController) to avoid breaking existing Flutter screens.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/variation-orders")
@PreAuthorize("isAuthenticated()")
public class VariationOrderController {

    private final VariationOrderService voService;

    public VariationOrderController(VariationOrderService voService) {
        this.voService = voService;
    }

    // ---- List ----

    @GetMapping
    @PreAuthorize("hasAuthority('VO_VIEW')")
    public ResponseEntity<ApiResponse<List<VariationOrderSummary>>> list(
            @PathVariable Long projectId) {
        List<ChangeOrder> orders = voService.listByProject(projectId);
        List<VariationOrderSummary> summaries = orders.stream()
                .map(VariationOrderSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Variation orders retrieved", summaries));
    }

    // ---- Detail ----

    @GetMapping("/{voId}")
    @PreAuthorize("hasAuthority('VO_VIEW')")
    public ResponseEntity<ApiResponse<VariationOrderResponse>> get(
            @PathVariable Long projectId, @PathVariable Long voId) {
        ChangeOrder co = voService.getVariationOrder(voId);
        List<ApprovalHistoryDto> history = voService.getApprovalHistory(voId)
                .stream().map(ApprovalHistoryDto::from).toList();
        PaymentScheduleDto schedule = null;
        try {
            ChangeOrderPaymentSchedule ps = voService.getPaymentSchedule(voId);
            schedule = PaymentScheduleDto.from(ps);
        } catch (IllegalStateException ignored) {
            // No payment schedule yet — VO not yet approved
        }
        return ResponseEntity.ok(ApiResponse.success("Variation order retrieved",
                VariationOrderResponse.from(co, history, schedule)));
    }

    // ---- Create ----

    @PostMapping
    @PreAuthorize("hasAuthority('VO_CREATE')")
    public ResponseEntity<ApiResponse<VariationOrderSummary>> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateVariationOrderRequest req) {
        Long userId = getCurrentUserId();
        ChangeOrder co = voService.createDraft(projectId, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variation order created", VariationOrderSummary.from(co)));
    }

    // ---- Submit ----

    @PostMapping("/{voId}/submit")
    @PreAuthorize("hasAuthority('VO_SUBMIT')")
    public ResponseEntity<ApiResponse<VariationOrderSummary>> submit(
            @PathVariable Long projectId, @PathVariable Long voId) {
        ChangeOrder co = voService.submit(voId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Variation order submitted for approval",
                VariationOrderSummary.from(co)));
    }

    // ---- Approve / Reject / Escalate / Return ----

    @PostMapping("/{voId}/approve")
    @PreAuthorize("hasAnyAuthority('VO_APPROVE_PM','VO_APPROVE_CM','VO_APPROVE_DIRECTOR')")
    public ResponseEntity<ApiResponse<VariationOrderSummary>> processApproval(
            @PathVariable Long projectId,
            @PathVariable Long voId,
            @Valid @RequestBody VOApprovalRequest req) {
        PortalUser user = getCurrentUser();
        ApprovalLevel level = resolveApprovalLevel(user);
        ChangeOrder co = voService.processApproval(voId, req, level, user.getId(),
                user.getFirstName() + " " + user.getLastName());
        return ResponseEntity.ok(ApiResponse.success("Approval action recorded",
                VariationOrderSummary.from(co)));
    }

    // ---- Payment schedule override ----

    @PutMapping("/{voId}/payment-schedule")
    @PreAuthorize("hasAuthority('VO_MANAGE_PAYMENT')")
    public ResponseEntity<ApiResponse<PaymentScheduleDto>> updateSchedule(
            @PathVariable Long projectId,
            @PathVariable Long voId,
            @Valid @RequestBody UpdatePaymentScheduleRequest req) {
        ChangeOrderPaymentSchedule schedule = voService.updatePaymentSchedule(voId, req);
        return ResponseEntity.ok(ApiResponse.success("Payment schedule updated",
                PaymentScheduleDto.from(schedule)));
    }

    // ---- Approval history ----

    @GetMapping("/{voId}/approval-history")
    @PreAuthorize("hasAuthority('VO_VIEW')")
    public ResponseEntity<ApiResponse<List<ApprovalHistoryDto>>> approvalHistory(
            @PathVariable Long projectId, @PathVariable Long voId) {
        List<ChangeOrderApprovalHistory> history = voService.getApprovalHistory(voId);
        return ResponseEntity.ok(ApiResponse.success("Approval history retrieved",
                history.stream().map(ApprovalHistoryDto::from).toList()));
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private PortalUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PortalUser user) return user;
        throw new IllegalStateException("Unable to extract user from authentication context");
    }

    /** Maps the user's portal role code to an ApprovalLevel. */
    private ApprovalLevel resolveApprovalLevel(PortalUser user) {
        if (user.getRole() == null) return ApprovalLevel.PM;
        String code = user.getRole().getCode() != null
                ? user.getRole().getCode().toUpperCase()
                : user.getRole().getName().toUpperCase();
        return switch (code) {
            case "DIRECTOR"            -> ApprovalLevel.DIRECTOR;
            case "COMMERCIAL_MANAGER"  -> ApprovalLevel.COMMERCIAL_MANAGER;
            default                    -> ApprovalLevel.PM;
        };
    }
}
