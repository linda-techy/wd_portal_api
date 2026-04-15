package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.DeductionRegisterDtos.*;
import com.wd.api.model.DeductionRegister;
import com.wd.api.service.DeductionRegisterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the Deduction Register.
 *
 * Base path: /api/projects/{projectId}/deductions
 */
@RestController
@RequestMapping("/api/projects/{projectId}/deductions")
@PreAuthorize("isAuthenticated()")
public class DeductionRegisterController {

    private final DeductionRegisterService deductionService;

    public DeductionRegisterController(DeductionRegisterService deductionService) {
        this.deductionService = deductionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEDUCTION_VIEW')")
    public ResponseEntity<ApiResponse<List<DeductionRegisterResponse>>> list(
            @PathVariable Long projectId) {
        List<DeductionRegister> deductions = deductionService.getByProject(projectId);
        List<DeductionRegisterResponse> responses = deductions.stream()
                .map(DeductionRegisterResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Deductions retrieved", responses));
    }

    @GetMapping("/{deductionId}")
    @PreAuthorize("hasAuthority('DEDUCTION_VIEW')")
    public ResponseEntity<ApiResponse<DeductionRegisterResponse>> get(
            @PathVariable Long projectId, @PathVariable Long deductionId) {
        DeductionRegister d = deductionService.getById(deductionId);
        return ResponseEntity.ok(ApiResponse.success("Deduction retrieved",
                DeductionRegisterResponse.from(d)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DEDUCTION_CREATE')")
    public ResponseEntity<ApiResponse<DeductionRegisterResponse>> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateDeductionRequest req) {
        DeductionRegister d = deductionService.create(projectId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deduction created", DeductionRegisterResponse.from(d)));
    }

    @PostMapping("/{deductionId}/decision")
    @PreAuthorize("hasAuthority('DEDUCTION_DECIDE')")
    public ResponseEntity<ApiResponse<DeductionRegisterResponse>> recordDecision(
            @PathVariable Long projectId,
            @PathVariable Long deductionId,
            @Valid @RequestBody DeductionDecisionRequest req) {
        DeductionRegister d = deductionService.recordDecision(deductionId, req);
        return ResponseEntity.ok(ApiResponse.success("Deduction decision recorded",
                DeductionRegisterResponse.from(d)));
    }

    @PostMapping("/{deductionId}/escalate")
    @PreAuthorize("hasAuthority('DEDUCTION_ESCALATE')")
    public ResponseEntity<ApiResponse<DeductionRegisterResponse>> escalate(
            @PathVariable Long projectId,
            @PathVariable Long deductionId,
            @Valid @RequestBody EscalateDeductionRequest req) {
        DeductionRegister d = deductionService.escalate(deductionId, req);
        return ResponseEntity.ok(ApiResponse.success("Deduction escalated",
                DeductionRegisterResponse.from(d)));
    }
}
