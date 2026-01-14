package com.wd.api.controller;

import com.wd.api.model.MaterialIndent;
import com.wd.api.service.MaterialIndentService;
import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.MaterialIndentSearchFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/indents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Adjust as per security config
public class MaterialIndentController {

    private final MaterialIndentService indentService;

    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> createIndent(
            @PathVariable Long projectId,
            @RequestBody MaterialIndent indent) {
        // TODO: Get Current User ID from Security Context
        Long currentUserId = 1L; // Placeholder or extract from Principal
        MaterialIndent created = indentService.createIndent(projectId, indent, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Indent created successfully", created));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> submitIndent(@PathVariable Long id) {
        MaterialIndent updated = indentService.submitIndent(id);
        return ResponseEntity.ok(ApiResponse.success("Indent submitted successfully", updated));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> approveIndent(@PathVariable Long id) {
        // TODO: Get Current User ID
        Long currentUserId = 1L;
        MaterialIndent approved = indentService.approveIndent(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Indent approved successfully", approved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> getIndent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Indent details", indentService.getIndentById(id)));
    }

    /**
     * NEW: Standardized search endpoint using MaterialIndentSearchFilter
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<Page<MaterialIndent>> searchIndents(@ModelAttribute MaterialIndentSearchFilter filter) {
        return ResponseEntity.ok(indentService.search(filter));
    }

    /**
     * DEPRECATED: Use /search endpoint instead
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'PROCUREMENT_MANAGER')")
    @Deprecated
    public ResponseEntity<ApiResponse<Page<MaterialIndent>>> searchIndentsOld(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<MaterialIndent> result = indentService.searchIndents(projectId, status, search,
                PageRequest.of(page, limit, sort));

        return ResponseEntity.ok(ApiResponse.success("Indents fetched successfully", result));
    }
}
