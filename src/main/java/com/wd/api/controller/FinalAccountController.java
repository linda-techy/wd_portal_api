package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.FinalAccountDtos.*;
import com.wd.api.model.FinalAccount;
import com.wd.api.service.FinalAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for Final Account management.
 *
 * Base path: /api/projects/{projectId}/final-account
 *
 * One final account per project (enforced by UNIQUE DB constraint).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/final-account")
@PreAuthorize("isAuthenticated()")
public class FinalAccountController {

    private final FinalAccountService finalAccountService;

    public FinalAccountController(FinalAccountService finalAccountService) {
        this.finalAccountService = finalAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_VIEW')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> get(
            @PathVariable Long projectId) {
        FinalAccount fa = finalAccountService.getByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Final account retrieved",
                FinalAccountResponse.from(fa)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_CREATE')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateFinalAccountRequest req) {
        FinalAccount fa = finalAccountService.create(projectId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Final account created", FinalAccountResponse.from(fa)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_EDIT')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> update(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateFinalAccountRequest req) {
        FinalAccount fa = finalAccountService.update(projectId, req);
        return ResponseEntity.ok(ApiResponse.success("Final account updated",
                FinalAccountResponse.from(fa)));
    }

    /** Recompute totals (accepted deductions, etc.) from live data before submitting. */
    @PostMapping("/recompute")
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_EDIT')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> recompute(
            @PathVariable Long projectId) {
        FinalAccount fa = finalAccountService.recomputeTotals(projectId);
        return ResponseEntity.ok(ApiResponse.success("Final account totals recomputed",
                FinalAccountResponse.from(fa)));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_SUBMIT')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> transition(
            @PathVariable Long projectId,
            @Valid @RequestBody FinalAccountStatusRequest req) {
        FinalAccount fa = finalAccountService.transitionStatus(projectId, req);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + fa.getStatus(),
                FinalAccountResponse.from(fa)));
    }

    @PostMapping("/release-retention")
    @PreAuthorize("hasAuthority('FINAL_ACCOUNT_RELEASE_RETENTION')")
    public ResponseEntity<ApiResponse<FinalAccountResponse>> releaseRetention(
            @PathVariable Long projectId,
            @Valid @RequestBody ReleaseRetentionRequest req) {
        FinalAccount fa = finalAccountService.releaseRetention(projectId, req);
        return ResponseEntity.ok(ApiResponse.success("Retention released",
                FinalAccountResponse.from(fa)));
    }

    /**
     * G-19: Translate a concurrent-edit collision (Hibernate optimistic-lock
     * failure on the @Version column) into a 409 Conflict so the UI can refetch
     * the latest state and prompt the user to redo their change instead of
     * silently overwriting another editor's work.
     */
    @ExceptionHandler({
            jakarta.persistence.OptimisticLockException.class,
            org.springframework.dao.OptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error("This final account was modified by another user. " +
                        "Please refresh and reapply your changes."));
    }
}
