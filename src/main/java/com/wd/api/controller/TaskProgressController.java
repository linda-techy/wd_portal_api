package com.wd.api.controller;

import com.wd.api.dto.TaskProgressUpdateRequest;
import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.service.TaskProgressUpdateService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * PATCH /api/projects/{projectId}/tasks/{taskId}/progress
 *
 * <p>Updates a task's progress percentage and auto-derives its status:
 * <ul>
 *   <li>0  → PENDING</li>
 *   <li>1–99 → IN_PROGRESS</li>
 *   <li>100  → COMPLETED (end date stamped if not already set)</li>
 * </ul>
 *
 * <p>Requires TASK_EDIT authority. Validation (0 ≤ progressPercent ≤ 100) is
 * handled by {@code @Valid} on the request body — invalid values yield 400.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}")
public class TaskProgressController {

    private static final Logger logger = LoggerFactory.getLogger(TaskProgressController.class);

    private final TaskProgressUpdateService progressService;
    private final PortalUserRepository portalUserRepo;

    public TaskProgressController(TaskProgressUpdateService progressService,
                                   PortalUserRepository portalUserRepo) {
        this.progressService = progressService;
        this.portalUserRepo = portalUserRepo;
    }

    @PatchMapping("/progress")
    @PreAuthorize("hasAuthority('TASK_EDIT')")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskProgressUpdateRequest request,
            Authentication auth) {
        try {
            PortalUser user = (auth != null)
                    ? portalUserRepo.findByEmail(auth.getName()).orElse(null)
                    : null;
            Task updated = progressService.updateProgress(
                    taskId, request.progressPercent(), request.note(), user);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        } catch (Exception e) {
            logger.error("Failed to update task progress for task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update progress"));
        }
    }
}
