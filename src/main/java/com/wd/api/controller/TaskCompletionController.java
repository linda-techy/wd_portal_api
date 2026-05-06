package com.wd.api.controller;

import com.wd.api.dto.scheduling.PendingApprovalRowDto;
import com.wd.api.dto.scheduling.RejectCompletionRequest;
import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import com.wd.api.service.scheduling.TaskCompletionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3 PR2 — Completion-gate REST endpoints.
 *
 * <p>POST /api/tasks/{id}/mark-complete    — site engineer (TASK_EDIT)
 * <p>POST /api/tasks/{id}/approve-completion — PM (TASK_COMPLETION_APPROVE)
 * <p>POST /api/tasks/{id}/reject-completion  — PM (TASK_COMPLETION_APPROVE)
 * <p>GET  /api/tasks/pending-pm-approval     — PM inbox (TASK_COMPLETION_APPROVE)
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskCompletionController {

    private final TaskCompletionService service;

    public TaskCompletionController(TaskCompletionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/mark-complete")
    @PreAuthorize("hasAuthority('TASK_EDIT')")
    public Task markComplete(@PathVariable Long id) {
        return service.markComplete(id, currentUserIdOrNull());
    }

    @PostMapping("/{id}/approve-completion")
    @PreAuthorize("hasAuthority('TASK_COMPLETION_APPROVE')")
    public Task approveCompletion(@PathVariable Long id) {
        return service.approveCompletion(id, currentUserIdOrNull());
    }

    @PostMapping("/{id}/reject-completion")
    @PreAuthorize("hasAuthority('TASK_COMPLETION_APPROVE')")
    public Task rejectCompletion(@PathVariable Long id,
                                 @Valid @RequestBody RejectCompletionRequest body) {
        return service.rejectCompletion(id, currentUserIdOrNull(), body.reason());
    }

    @GetMapping("/pending-pm-approval")
    @PreAuthorize("hasAuthority('TASK_COMPLETION_APPROVE')")
    public List<PendingApprovalRowDto> pendingApprovalInbox() {
        return service.findPendingApprovalsForUser(currentUserIdOrNull());
    }

    /**
     * Mirrors {@code BoqInvoiceController.getCurrentUserId} but tolerates
     * a non-PortalUser principal (e.g. {@code @WithMockUser} string principal
     * in MockMvc tests) by returning null instead of throwing — the FSM
     * service signature already accepts a nullable userId.
     */
    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof PortalUser u) return u.getId();
        return null;
    }
}
