package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.ChangeOrderResponse;
import com.wd.api.model.ChangeOrder;
import com.wd.api.model.ChangeOrderLineItem;
import com.wd.api.model.PortalUser;
import com.wd.api.model.enums.ChangeOrderType;
import com.wd.api.service.ChangeOrderService;
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
import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for Change Order management.
 * All COs operate on an already-approved BOQ document (R-003).
 */
@RestController
@RequestMapping("/api/change-orders")
@PreAuthorize("isAuthenticated()")
public class ChangeOrderController {

    private static final Logger logger = LoggerFactory.getLogger(ChangeOrderController.class);

    private final ChangeOrderService changeOrderService;

    public ChangeOrderController(ChangeOrderService changeOrderService) {
        this.changeOrderService = changeOrderService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("OK",
                    ChangeOrderResponse.from(changeOrderService.getChangeOrder(id))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<ChangeOrderResponse>>> getByProject(
            @PathVariable Long projectId) {
        try {
            List<ChangeOrderResponse> cos = changeOrderService.getProjectChangeOrders(projectId)
                    .stream().map(ChangeOrderResponse::from).toList();
            return ResponseEntity.ok(ApiResponse.success("OK", cos));
        } catch (Exception e) {
            logger.error("Failed to list change orders for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOQ_CREATE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> create(
            @Valid @RequestBody CreateChangeOrderRequest request) {
        try {
            Long userId = getCurrentUserId();
            List<ChangeOrderLineItem> lineItems = request.lineItems().stream()
                    .map(this::toLineItem).toList();
            ChangeOrder co = changeOrderService.createChangeOrder(
                    request.projectId(), request.coType(),
                    request.title(), request.description(), request.justification(),
                    lineItems, userId);
            return ResponseEntity.status(201).body(
                    ApiResponse.success("Change order created", ChangeOrderResponse.from(co)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create change order", e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> submit(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order submitted",
                    ChangeOrderResponse.from(changeOrderService.submit(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to submit change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/approve-internal")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> approveInternally(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order internally approved",
                    ChangeOrderResponse.from(changeOrderService.approveInternally(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to internally approve change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/reject-internal")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> rejectInternally(
            @PathVariable Long id, @RequestBody RejectInternalRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order returned for revision",
                    ChangeOrderResponse.from(changeOrderService.rejectInternally(id, getCurrentUserId(), request.reason()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to internally reject change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/send-to-customer")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> sendToCustomer(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order sent to customer",
                    ChangeOrderResponse.from(changeOrderService.sendToCustomer(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to send change order {} to customer", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAuthority('BOQ_EDIT')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> start(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order started",
                    ChangeOrderResponse.from(changeOrderService.startProgress(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to start change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('BOQ_EDIT')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> complete(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order completed",
                    ChangeOrderResponse.from(changeOrderService.complete(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to complete change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>> close(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Change order closed",
                    ChangeOrderResponse.from(changeOrderService.close(id, getCurrentUserId()))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to close change order {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    // ---- Helpers ----

    private ChangeOrderLineItem toLineItem(LineItemRequest r) {
        ChangeOrderLineItem li = new ChangeOrderLineItem();
        li.setDescription(r.description());
        li.setUnit(r.unit());
        li.setOriginalQuantity(r.originalQuantity() != null ? r.originalQuantity() : BigDecimal.ZERO);
        li.setNewQuantity(r.newQuantity() != null ? r.newQuantity() : BigDecimal.ZERO);
        li.setDeltaQuantity(li.getNewQuantity().subtract(li.getOriginalQuantity()));
        li.setOriginalRate(r.originalRate() != null ? r.originalRate() : BigDecimal.ZERO);
        li.setNewRate(r.newRate() != null ? r.newRate() : BigDecimal.ZERO);
        li.setUnitRate(r.unitRate() != null ? r.unitRate() : BigDecimal.ZERO);
        li.setLineAmountExGst(r.lineAmountExGst());
        li.setSpecifications(r.specifications());
        li.setNotes(r.notes());
        return li;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("Not authenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof PortalUser user) return user.getId();
        throw new IllegalStateException("Cannot extract user ID");
    }

    // ---- Request DTOs ----

    public record CreateChangeOrderRequest(
            @NotNull Long projectId,
            @NotNull ChangeOrderType coType,
            @NotBlank @Size(max = 255) String title,
            String description,
            String justification,
            @NotNull @Size(min = 1) List<LineItemRequest> lineItems
    ) {}

    public record RejectInternalRequest(
            @NotBlank String reason
    ) {}

    public record LineItemRequest(
            @NotBlank @Size(max = 255) String description,
            String unit,
            BigDecimal originalQuantity,
            BigDecimal newQuantity,
            BigDecimal originalRate,
            BigDecimal newRate,
            BigDecimal unitRate,
            @NotNull BigDecimal lineAmountExGst,
            String specifications,
            String notes
    ) {}
}
