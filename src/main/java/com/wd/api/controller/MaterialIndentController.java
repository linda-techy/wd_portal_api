package com.wd.api.controller;

import com.wd.api.model.MaterialIndent;
import com.wd.api.model.PortalUser;
import com.wd.api.service.MaterialIndentService;
import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.MaterialIndentSearchFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/indents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaterialIndentController {

    private static final Logger logger = LoggerFactory.getLogger(MaterialIndentController.class);

    private final MaterialIndentService indentService;

    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> createIndent(
            @PathVariable Long projectId,
            @RequestBody MaterialIndent indent) {
        try {
            Long currentUserId = getCurrentUserId();
            MaterialIndent created = indentService.createIndent(projectId, indent, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Indent created successfully", created));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating indent for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create indent for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to create indent"));
        }
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> submitIndent(@PathVariable Long id) {
        try {
            MaterialIndent updated = indentService.submitIndent(id);
            return ResponseEntity.ok(ApiResponse.success("Indent submitted successfully", updated));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error submitting indent {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to submit indent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to submit indent"));
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> approveIndent(@PathVariable Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            MaterialIndent approved = indentService.approveIndent(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Indent approved successfully", approved));
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error approving indent {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to approve indent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to approve indent"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<ApiResponse<MaterialIndent>> getIndent(@PathVariable Long id) {
        try {
            MaterialIndent indent = indentService.getIndentById(id);
            if (indent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Indent not found with ID: " + id));
            }
            return ResponseEntity.ok(ApiResponse.success("Indent details", indent));
        } catch (Exception e) {
            logger.error("Failed to fetch indent {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch indent"));
        }
    }

    /**
     * NEW: Standardized search endpoint using MaterialIndentSearchFilter
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'SITE_ENGINEER', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<?> searchIndents(@ModelAttribute MaterialIndentSearchFilter filter) {
        try {
            Page<MaterialIndent> results = indentService.search(filter);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid search filter for indents: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to search indents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to search indents"));
        }
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
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Page<MaterialIndent> result = indentService.searchIndents(projectId, status, search,
                    PageRequest.of(page, limit, sort));
            return ResponseEntity.ok(ApiResponse.success("Indents fetched successfully", result));
        } catch (Exception e) {
            logger.error("Failed to search indents (legacy): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch indents"));
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PortalUser) {
            return ((PortalUser) authentication.getPrincipal()).getId();
        }
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                logger.warn("Invalid user ID format in authentication principal: {}", 
                    authentication.getName());
                // Name is not a numeric ID (e.g., email); return null safely
            }
        }
        return null;
    }
}
