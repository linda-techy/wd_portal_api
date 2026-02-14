package com.wd.api.controller;

import com.wd.api.dto.*;
import com.wd.api.model.BoqItem;
import com.wd.api.model.BoqWorkType;
import com.wd.api.service.BoqCategoryService;
import com.wd.api.service.BoqService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BOQ Controller - REST endpoints for BOQ management.
 * Enforces RBAC and delegates to service layer for business logic.
 */
@RestController
@RequestMapping("/api/boq")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class BoqController {

    private final BoqService boqService;
    private final BoqCategoryService categoryService;

    public BoqController(BoqService boqService, BoqCategoryService categoryService) {
        this.boqService = boqService;
        this.categoryService = categoryService;
    }

    // ---- CRUD Operations ----

    @PostMapping
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
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to create BOQ item: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
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
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to update BOQ item: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            boqService.softDeleteBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item deleted successfully", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete BOQ item: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoqItemResponse>> getBoqItem(@PathVariable Long id) {
        try {
            BoqItemResponse response = boqService.getBoqItemById(id);
            return ResponseEntity.ok(ApiResponse.success("BOQ item retrieved successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("BOQ item not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch BOQ item: " + e.getMessage()));
        }
    }

    // ---- Status Workflow ----

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> approveBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.approveBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item approved successfully", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to approve BOQ item: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> lockBoqItem(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.lockBoqItem(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item locked successfully", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to lock BOQ item: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<BoqItemResponse>> markAsCompleted(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            BoqItemResponse response = boqService.markAsCompleted(id, userId);
            return ResponseEntity.ok(ApiResponse.success("BOQ item marked as completed", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to complete BOQ item: " + e.getMessage()));
        }
    }

    // ---- Execution & Billing ----

    @PatchMapping("/{id}/execute")
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
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to record execution: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/bill")
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
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to record billing: " + e.getMessage()));
        }
    }

    // ---- Queries ----

    @GetMapping("/search")
    public ResponseEntity<Page<BoqItem>> searchBoqItems(@ModelAttribute BoqSearchFilter filter) {
        try {
            Page<BoqItem> items = boqService.searchBoqItems(filter);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<BoqItemResponse>>> getProjectBoq(@PathVariable Long projectId) {
        try {
            List<BoqItemResponse> items = boqService.getProjectBoq(projectId);
            return ResponseEntity.ok(ApiResponse.success("BOQ items retrieved successfully", items));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch BOQ items: " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}/financial-summary")
    public ResponseEntity<ApiResponse<BoqFinancialSummary>> getFinancialSummary(@PathVariable Long projectId) {
        try {
            BoqFinancialSummary summary = boqService.getFinancialSummary(projectId);
            return ResponseEntity.ok(ApiResponse.success("Financial summary retrieved successfully", summary));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch financial summary: " + e.getMessage()));
        }
    }

    @GetMapping("/work-types")
    public ResponseEntity<ApiResponse<List<BoqWorkType>>> getWorkTypes() {
        try {
            List<BoqWorkType> workTypes = boqService.getAllWorkTypes();
            return ResponseEntity.ok(ApiResponse.success("Work types retrieved successfully", workTypes));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch work types: " + e.getMessage()));
        }
    }

    // ---- Category Management ----

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<BoqCategoryDto>> createCategory(@Valid @RequestBody CreateBoqCategoryRequest request) {
        try {
            Long userId = getCurrentUserId();
            BoqCategoryDto category = categoryService.createCategory(request, userId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.success("Category created successfully", category));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to create category: " + e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}/categories")
    public ResponseEntity<ApiResponse<List<BoqCategoryDto>>> getCategories(@PathVariable Long projectId) {
        try {
            List<BoqCategoryDto> categories = categoryService.getCategoriesByProject(projectId);
            return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch categories: " + e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long categoryId) {
        try {
            Long userId = getCurrentUserId();
            categoryService.softDeleteCategory(categoryId, userId);
            return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete category: " + e.getMessage()));
        }
    }

    // ---- Helper Methods ----

    /**
     * Get current authenticated user ID from security context.
     * TODO: Implement proper user extraction from JWT/Security Principal.
     * For now, returns 1L as placeholder.
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                return (Long) auth.getPrincipal();
            }
            // TODO: Extract user ID from JWT claims or custom UserDetails
            return 1L; // Placeholder - replace with actual user ID extraction
        } catch (Exception e) {
            return 1L; // Fallback
        }
    }
}
