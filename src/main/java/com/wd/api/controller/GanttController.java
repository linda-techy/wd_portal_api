package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.Task;
import com.wd.api.service.GanttService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Gantt / Project Timeline endpoints.
 *
 * GET  /api/projects/{projectId}/schedule/gantt  — TASK_VIEW
 * PUT  /api/projects/{projectId}/tasks/{taskId}/schedule — TASK_EDIT
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class GanttController {

    private static final Logger logger = LoggerFactory.getLogger(GanttController.class);

    @Autowired
    private GanttService ganttService;

    // ──────────────────────────────────────────────────────────────────────────
    // GET gantt data for a project
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/schedule/gantt")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGanttData(
            @PathVariable Long projectId) {
        try {
            Map<String, Object> data = ganttService.getGanttData(projectId);
            return ResponseEntity.ok(ApiResponse.success("Gantt data retrieved successfully", data));
        } catch (Exception e) {
            logger.error("Error fetching Gantt data for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT schedule for a single task
    // ──────────────────────────────────────────────────────────────────────────

    @PutMapping("/tasks/{taskId}/schedule")
    @PreAuthorize("hasAnyAuthority('TASK_EDIT', 'TASK_CREATE')")
    public ResponseEntity<ApiResponse<Task>> updateTaskSchedule(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        try {
            LocalDate startDate = parseDate(body.get("startDate"));
            LocalDate endDate = parseDate(body.get("endDate"));
            Integer progressPercent = body.get("progressPercent") != null
                    ? Integer.parseInt(body.get("progressPercent").toString())
                    : null;

            // S2 PR2: legacy dependsOnTaskId field is silently ignored.
            // Predecessor edits flow through PUT /tasks/{id}/predecessors (S1).
            // Logged at INFO so we can observe rolling-deploy traffic and
            // confirm clients have stopped sending the field.
            if (body.containsKey("dependsOnTaskId")) {
                logger.info("Ignored deprecated field 'dependsOnTaskId' on PUT /tasks/{}/schedule", taskId);
            }

            Task updated = ganttService.updateTaskSchedule(taskId, startDate, endDate, progressPercent);
            return ResponseEntity.ok(ApiResponse.success("Task schedule updated successfully", updated));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid schedule update for task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating schedule for task {}", taskId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private LocalDate parseDate(Object value) {
        if (value == null) return null;
        return LocalDate.parse(value.toString());
    }
}
