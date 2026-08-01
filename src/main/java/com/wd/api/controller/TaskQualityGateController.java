package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.TaskQualityGateDTO;
import com.wd.api.model.PortalUser;
import com.wd.api.model.TaskQualityGate;
import com.wd.api.service.TaskQualityGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Per-task ITP (Inspection-Test Plan) quality gates.
 *
 * Three gates per task: PRELIMINARY, IN_PROGRESS, FINAL. The same assigned site
 * engineer signs each one off as the work progresses; the next can't be entered
 * until the previous PASSES (or is marked NA with justification). A task can
 * not be marked COMPLETED until its FINAL gate is PASSED.
 *
 * Auth: signing a gate requires `TASK_QC_SIGNOFF` (site engineers, supervisors,
 * QA/safety, foremen, MEP supervisors, PMs, admins).
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/quality-gates")
public class TaskQualityGateController {

    private static final Logger logger = LoggerFactory.getLogger(TaskQualityGateController.class);

    private final TaskQualityGateService service;

    public TaskQualityGateController(TaskQualityGateService service) {
        this.service = service;
    }

    /** List all 3 gates for a task in ITP order. */
    @GetMapping
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<ApiResponse<List<TaskQualityGateDTO>>> list(@PathVariable Long taskId) {
        List<TaskQualityGateDTO> gates = service.getGates(taskId)
                .stream()
                .map(TaskQualityGateDTO::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Gates retrieved", gates));
    }

    /**
     * Sign off a gate.
     * Body: { "gateType": "PRELIMINARY|IN_PROGRESS|FINAL",
     *         "status":   "PASSED|FAILED|NA",
     *         "notes":    "optional",
     *         "failureReason": "required when status=FAILED" }
     */
    @PostMapping("/sign-off")
    @PreAuthorize("hasAuthority('TASK_QC_SIGNOFF')")
    public ResponseEntity<ApiResponse<TaskQualityGateDTO>> signOff(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String typeStr   = body.get("gateType");
            String statusStr = body.get("status");
            if (typeStr == null || statusStr == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("gateType and status are required."));
            }
            TaskQualityGate.GateType type = TaskQualityGate.GateType.valueOf(typeStr);
            TaskQualityGate.Status status = TaskQualityGate.Status.valueOf(statusStr);

            PortalUser user = (PortalUser) auth.getPrincipal();
            TaskQualityGate saved = service.signOff(
                    taskId, type, status,
                    body.get("notes"),
                    body.get("failureReason"),
                    user.getId());
            return ResponseEntity.ok(ApiResponse.success(
                    "Gate signed off as " + status, TaskQualityGateDTO.from(saved)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Sign-off failed for task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to sign off gate: " + e.getMessage()));
        }
    }
}
