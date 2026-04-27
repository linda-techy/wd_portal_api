package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.dpc.CreateDpcCustomizationCatalogItemRequest;
import com.wd.api.dto.dpc.DpcCustomizationCatalogItemDto;
import com.wd.api.dto.dpc.DpcCustomizationCatalogSearchFilter;
import com.wd.api.dto.dpc.UpdateDpcCustomizationCatalogItemRequest;
import com.wd.api.service.dpc.DpcCustomizationCatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the admin-managed DPC-customization catalog.
 *
 * <p>Read endpoints are gated behind {@code DPC_CUSTOMIZATION_CATALOG_VIEW}
 * so any DPC editor can search the picker. Write endpoints (create / patch /
 * soft-delete) require {@code DPC_CUSTOMIZATION_CATALOG_MANAGE}.
 *
 * <p>Exception mapping: {@code IllegalArgumentException} -> 404 (not found)
 * or 400 (validation), {@code IllegalStateException} -> 422 (invariant
 * violation, e.g. duplicate code), generic -> 500.
 */
@RestController
@RequestMapping("/api/dpc-customization-catalog")
@PreAuthorize("isAuthenticated()")
public class DpcCustomizationCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(DpcCustomizationCatalogController.class);

    private final DpcCustomizationCatalogService catalogService;

    public DpcCustomizationCatalogController(DpcCustomizationCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DPC_CUSTOMIZATION_CATALOG_VIEW')")
    public ResponseEntity<ApiResponse<Page<DpcCustomizationCatalogItemDto>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        try {
            DpcCustomizationCatalogSearchFilter filter = new DpcCustomizationCatalogSearchFilter(
                    search, category, isActive, page, size, sortBy, sortDirection);
            Page<DpcCustomizationCatalogItemDto> result = catalogService.search(filter);
            return ResponseEntity.ok(ApiResponse.success("DPC customization catalog retrieved", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to search DPC customization catalog", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while searching the catalog"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DPC_CUSTOMIZATION_CATALOG_VIEW')")
    public ResponseEntity<ApiResponse<DpcCustomizationCatalogItemDto>> getById(@PathVariable Long id) {
        try {
            DpcCustomizationCatalogItemDto dto = catalogService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("DPC customization catalog item retrieved", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch DPC customization catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while fetching the catalog item"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DPC_CUSTOMIZATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<DpcCustomizationCatalogItemDto>> create(
            @Valid @RequestBody CreateDpcCustomizationCatalogItemRequest request) {
        try {
            DpcCustomizationCatalogItemDto dto = catalogService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("DPC customization catalog item created", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create DPC customization catalog item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while creating the catalog item"));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DPC_CUSTOMIZATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<DpcCustomizationCatalogItemDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDpcCustomizationCatalogItemRequest request) {
        try {
            DpcCustomizationCatalogItemDto dto = catalogService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("DPC customization catalog item updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update DPC customization catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the catalog item"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DPC_CUSTOMIZATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        try {
            catalogService.softDelete(id);
            return ResponseEntity.ok(ApiResponse.success("DPC customization catalog item deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete DPC customization catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while deleting the catalog item"));
        }
    }
}
