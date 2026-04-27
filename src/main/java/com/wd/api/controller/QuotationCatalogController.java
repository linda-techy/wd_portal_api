package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.quotation.CreateQuotationCatalogItemRequest;
import com.wd.api.dto.quotation.QuotationCatalogItemDto;
import com.wd.api.dto.quotation.QuotationCatalogSearchFilter;
import com.wd.api.dto.quotation.UpdateQuotationCatalogItemRequest;
import com.wd.api.service.QuotationCatalogService;
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
 * REST controller for the admin-managed quotation-item catalog.
 *
 * <p>Read endpoints are gated behind {@code QUOTATION_CATALOG_VIEW} so any
 * lead-team user can search the picker. Write endpoints (create / patch /
 * soft-delete) require {@code QUOTATION_CATALOG_MANAGE}.
 *
 * <p>Exception mapping: {@code IllegalArgumentException} -> 404 (not found)
 * or 400 (validation), {@code IllegalStateException} -> 422 (invariant
 * violation, e.g. duplicate code), generic -> 500.
 */
@RestController
@RequestMapping("/api/quotation-catalog")
@PreAuthorize("isAuthenticated()")
public class QuotationCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(QuotationCatalogController.class);

    private final QuotationCatalogService catalogService;

    public QuotationCatalogController(QuotationCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('QUOTATION_CATALOG_VIEW')")
    public ResponseEntity<ApiResponse<Page<QuotationCatalogItemDto>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        try {
            QuotationCatalogSearchFilter filter = new QuotationCatalogSearchFilter(
                    search, category, isActive, page, size, sortBy, sortDirection);
            Page<QuotationCatalogItemDto> result = catalogService.search(filter);
            return ResponseEntity.ok(ApiResponse.success("Quotation catalog retrieved", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to search quotation catalog", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while searching the catalog"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTATION_CATALOG_VIEW')")
    public ResponseEntity<ApiResponse<QuotationCatalogItemDto>> getById(@PathVariable Long id) {
        try {
            QuotationCatalogItemDto dto = catalogService.getById(id);
            return ResponseEntity.ok(ApiResponse.success("Quotation catalog item retrieved", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch quotation catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while fetching the catalog item"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUOTATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<QuotationCatalogItemDto>> create(
            @Valid @RequestBody CreateQuotationCatalogItemRequest request) {
        try {
            QuotationCatalogItemDto dto = catalogService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Quotation catalog item created", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create quotation catalog item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while creating the catalog item"));
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<QuotationCatalogItemDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuotationCatalogItemRequest request) {
        try {
            QuotationCatalogItemDto dto = catalogService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Quotation catalog item updated", dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update quotation catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while updating the catalog item"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTATION_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        try {
            catalogService.softDelete(id);
            return ResponseEntity.ok(ApiResponse.success("Quotation catalog item deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to delete quotation catalog item {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An internal error occurred while deleting the catalog item"));
        }
    }
}
