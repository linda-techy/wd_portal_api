package com.wd.api.controller;

import com.wd.api.dto.*;
import com.wd.api.model.BoqAuditLog;
import com.wd.api.model.BoqWorkType;
import com.wd.api.model.PortalUser;
import com.wd.api.service.BoqCategoryService;
import com.wd.api.service.BoqExportService;
import com.wd.api.service.BoqService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * BOQ Controller - REST endpoints for BOQ management.
 * Enforces RBAC and delegates to service layer for business logic.
 */
@RestController
@RequestMapping("/api/boq")
@PreAuthorize("isAuthenticated()")
public class BoqController {

    private static final Logger logger = LoggerFactory.getLogger(BoqController.class);

    private final BoqService boqService;
    private final BoqCategoryService categoryService;
    private final BoqExportService exportService;

    public BoqController(BoqService boqService, BoqCategoryService categoryService,
                         BoqExportService exportService) {
        this.boqService = boqService;
        this.categoryService = categoryService;
        this.exportService = exportService;
    }

    // ---- CRUD Operations ----

    @PostMapping
    @PreAuthorize("hasAuthority('BOQ_CREATE')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> createBoqItem(@Valid @RequestBody CreateBoqItemRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.createBoqItem(request, userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("BOQ item created successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create BOQ item", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while creating the BOQ item"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_EDIT')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> updateBoqItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoqItemRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.updateBoqItem(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item updated successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while updating the BOQ item"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            boqService.softDeleteBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item deleted successfully", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while deleting the BOQ item"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> getBoqItem(@PathVariable Long id) {
        try {
            BoqItemResponse response = boqService.getBoqItemById(id);
            return ResponseEntity.ok(ApiResponse.success("BOQ item retrieved successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("BOQ item not found: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching the BOQ item"));
        }
    }

    // ---- Status Workflow ----

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> approveBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.approveBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item approved successfully", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to approve BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while approving the BOQ item"));
        }
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> lockBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.lockBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item locked successfully", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to lock BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while locking the BOQ item"));
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('BOQ_APPROVE')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> markAsCompleted(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.markAsCompleted(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item marked as completed", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to complete BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while completing the BOQ item"));
        }
    }

    // ---- Execution & Billing ----

    @PatchMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('BOQ_EDIT')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> recordExecution(
            @PathVariable Long id,
            @Valid @RequestBody RecordExecutionRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.recordExecution(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Execution recorded successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to record execution for BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while recording execution"));
        }
    }

    @PatchMapping("/{id}/bill")
    @PreAuthorize("hasAuthority('BOQ_EDIT')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> recordBilling(
            @PathVariable Long id,
            @Valid @RequestBody RecordExecutionRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.recordBilling(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Billing recorded successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to record billing for BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while recording billing"));
        }
    }

    // ---- Queries ----

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<Page<BoqItemResponse>>> searchBoqItems(@ModelAttribute BoqSearchFilter filter) {
        try {
            Page<BoqItemResponse> items = boqService.searchBoqItems(filter);
            return ResponseEntity.ok(ApiResponse.success("BOQ items retrieved successfully", items));
        } catch (Exception e) {
            logger.error("Failed to search BOQ items", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while searching BOQ items"));
        }
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqItemResponse>>> getProjectBoq(@PathVariable Long projectId) {
        try {
            List<BoqItemResponse> items = boqService.getProjectBoq(projectId);
            return ResponseEntity.ok(ApiResponse.success("BOQ items retrieved successfully", items));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ items for project {}", projectId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching BOQ items"));
        }
    }

    @GetMapping("/project/{projectId}/financial-summary")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<BoqFinancialSummary>> getFinancialSummary(@PathVariable Long projectId) {
        try {
            BoqFinancialSummary summary = boqService.getFinancialSummary(projectId);
            return ResponseEntity.ok(ApiResponse.success("Financial summary retrieved successfully", summary));
        } catch (Exception e) {
            logger.error("Failed to fetch financial summary for project {}", projectId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching the financial summary"));
        }
    }

    @GetMapping("/project/{projectId}/export")
    @PreAuthorize("hasAuthority('BOQ_EXPORT')")
    public ResponseEntity<byte[]> exportBoqExcel(@PathVariable Long projectId) {
        try {
            byte[] bytes = exportService.generateExcel(projectId);
            String filename = "boq_project_" + projectId + "_" + LocalDate.now() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            logger.error("Failed to export BOQ for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/work-types")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqWorkType>>> getWorkTypes() {
        try {
            List<BoqWorkType> workTypes = boqService.getAllWorkTypes();
            return ResponseEntity.ok(ApiResponse.success("Work types retrieved successfully", workTypes));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ work types", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching work types"));
        }
    }

    // ---- Category Management ----

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('BOQ_CREATE')")
    public ResponseEntity<ApiResponse<BoqCategoryDto>> createCategory(@Valid @RequestBody CreateBoqCategoryRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqCategoryDto category = categoryService.createCategory(request, userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("Category created successfully", category));
        } catch (Exception e) {
            logger.error("Failed to create BOQ category", e);
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to create category"));
        }
    }

    @GetMapping("/project/{projectId}/categories")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqCategoryDto>>> getCategories(@PathVariable Long projectId) {
        try {
            List<BoqCategoryDto> categories = categoryService.getCategoriesByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ categories for project {}", projectId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching categories"));
        }
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAuthority('BOQ_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        try {
            Long userId = getCurrentUserId();
            categoryService.softDeleteCategory(categoryId, userId);
            return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete BOQ category {}", categoryId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while deleting the category"));
        }
    }

    // ---- Audit Log ----

    @GetMapping("/{id}/audit-log")
    @PreAuthorize("hasAuthority('BOQ_VIEW')")
    public ResponseEntity<ApiResponse<List<BoqAuditLog>>> getAuditLog(@PathVariable Long id) {
        try {
            List<BoqAuditLog> logs = boqService.getAuditLog(id);
            return ResponseEntity.ok(ApiResponse.success("Audit log retrieved successfully", logs));
        } catch (Exception e) {
            logger.error("Failed to fetch audit log for BOQ item {}", id, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("An internal error occurred while fetching the audit log"));
        }
    }

    // ---- Exception Handlers ----
    
    /**
     * Handle concurrent modification conflicts.
     * Returns user-friendly error message when two users edit same record.
     */
    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleConcurrentModification(Exception ex) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(
                    "This BOQ item was modified by another user. Please refresh and try again."
                ));
    }

    // ---- Helper Methods ----

    /**
     * Get current authenticated user ID from security context.
     * The JwtAuthenticationFilter sets a PortalUser as the principal for portal users.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof PortalUser portalUser) {
            return portalUser.getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication context");
    }

    @PostMapping("/{id}/correct-execution")
    @PreAuthorize("hasAuthority('BOQ_CORRECT')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> correctExecution(
            @PathVariable Long id,
            @Valid @RequestBody CorrectionRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.correctExecution(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Execution corrected successfully", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to correct execution for BOQ item {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred while correcting execution"));
        }
    }
}
