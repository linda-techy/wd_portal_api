package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.*;
import com.wd.api.estimation.service.EstimationSubResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * CRUD endpoints for the 4 estimation sub-resource types.
 *
 * Base path: /api/lead-estimations/{estimationId}/{subResourceType}
 * where {subResourceType} is one of: inclusions, exclusions, assumptions, payment-milestones
 */
@RestController
@RequestMapping("/api/lead-estimations/{estimationId}/{subResourceType}")
@PreAuthorize("isAuthenticated()")
public class EstimationSubResourceController {

    private final EstimationSubResourceService service;

    public EstimationSubResourceController(EstimationSubResourceService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<List<EstimationSubResourceResponse>>> list(
            @PathVariable UUID estimationId,
            @PathVariable String subResourceType) {
        SubResourceType type = SubResourceType.forPath(subResourceType);
        return ResponseEntity.ok(ApiResponse.success("OK", service.list(estimationId, type)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<EstimationSubResourceResponse>> get(
            @PathVariable UUID estimationId,
            @PathVariable String subResourceType,
            @PathVariable UUID id) {
        SubResourceType type = SubResourceType.forPath(subResourceType);
        return ResponseEntity.ok(ApiResponse.success("OK", service.get(estimationId, type, id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<EstimationSubResourceResponse>> create(
            @PathVariable UUID estimationId,
            @PathVariable String subResourceType,
            @Valid @RequestBody EstimationSubResourceRequest req) {
        SubResourceType type = SubResourceType.forPath(subResourceType);
        EstimationSubResourceResponse created = service.create(estimationId, type, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<EstimationSubResourceResponse>> update(
            @PathVariable UUID estimationId,
            @PathVariable String subResourceType,
            @PathVariable UUID id,
            @Valid @RequestBody EstimationSubResourceRequest req) {
        SubResourceType type = SubResourceType.forPath(subResourceType);
        return ResponseEntity.ok(ApiResponse.success("Updated", service.update(estimationId, type, id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID estimationId,
            @PathVariable String subResourceType,
            @PathVariable UUID id) {
        SubResourceType type = SubResourceType.forPath(subResourceType);
        service.delete(estimationId, type, id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
