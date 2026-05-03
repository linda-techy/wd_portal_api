package com.wd.api.estimation.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.estimation.dto.*;
import com.wd.api.estimation.service.LeadEstimationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lead-estimations")
@PreAuthorize("isAuthenticated()")
public class LeadEstimationController {

    private final LeadEstimationService service;

    public LeadEstimationController(LeadEstimationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> create(
            @Valid @RequestBody LeadEstimationCreateRequest req) {
        LeadEstimationDetailResponse created = service.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Estimation saved", created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<List<LeadEstimationSummaryResponse>>> listByLead(
            @RequestParam Long leadId) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.listByLead(leadId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_VIEW')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("OK", service.get(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAD_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Estimation deleted", null));
    }

    @PatchMapping("/{id}/mark-sent")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> markSent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Marked SENT", service.markSent(id)));
    }

    @PatchMapping("/{id}/mark-accepted")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> markAccepted(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Marked ACCEPTED", service.markAccepted(id)));
    }

    @PatchMapping("/{id}/mark-rejected")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> markRejected(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Marked REJECTED", service.markRejected(id)));
    }

    @PatchMapping("/{id}/mark-draft")
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE', 'LEAD_EDIT')")
    public ResponseEntity<ApiResponse<LeadEstimationDetailResponse>> revertToDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Reverted to DRAFT", service.revertToDraft(id)));
    }
}
