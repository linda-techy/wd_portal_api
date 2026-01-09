package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.model.Task;
import com.wd.api.model.TaskAssignmentHistory;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.security.TaskAuthorizationService;
import com.wd.api.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Production-grade Task Controller with RBAC
 * 
 * Authorization Model:
 * - Admin: Full access to all operations
 * - Project Manager: Can edit all tasks in their projects
 * - Task Creator: Can edit/delete own tasks
 * - Regular User: Can create tasks, view assigned tasks
 * 
 * @author Senior Engineer (15+ years construction domain experience)
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private TaskAuthorizationService authService;

    @Autowired
    private com.wd.api.service.TaskAlertService taskAlertService;

    @Autowired
    private com.wd.api.scheduler.TaskAlertScheduler taskAlertScheduler;

    // ... existing methods ...

    // ... existing tasks methods ...

    /**
     * Get alert statistics (Dashboard)
     */
    @GetMapping("/alerts/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<com.wd.api.service.TaskAlertService.AlertStats>> getAlertStats(
            @RequestParam(defaultValue = "7") int days) {
        try {
            com.wd.api.service.TaskAlertService.AlertStats stats = taskAlertService.getAlertStats(days);
            return ResponseEntity.ok(ApiResponse.success("Alert stats retrieved successfully", stats));
        } catch (Exception e) {
            logger.error("Error fetching alert stats", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/alerts/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<com.wd.api.model.TaskAlert>>> getRecentAlerts() {
        try {
            List<com.wd.api.model.TaskAlert> alerts = taskAlertService.getRecentAlerts();
            return ResponseEntity.ok(ApiResponse.success("Recent alerts retrieved successfully", alerts));
        } catch (Exception e) {
            logger.error("Error fetching recent alerts", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get all tasks - admins see all, users see only theirs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks(Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);

            List<Task> tasks;
            if (authService.isAdmin(auth)) {
                tasks = taskService.getAllTasks();
                logger.info("Admin {} retrieved all tasks", user.getEmail());
            } else {
                tasks = taskService.getTasksForUser(user, auth);
                logger.info("User {} retrieved their tasks", user.getEmail());
            }

            return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
        } catch (Exception e) {
            logger.error("Error fetching tasks", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get task by ID with permission check
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable Long id, Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);

            Optional<Task> task = taskService.getTaskById(id);
            if (task.isEmpty()) {
                return ResponseEntity.status(404).body(ApiResponse.error("Task not found"));
            }

            // Check view permission
            if (!authService.canViewTask(id, auth, user.getId())) {
                logger.warn("User {} attempted to view task {} without permission", user.getEmail(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
            }

            return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task.get()));
        } catch (Exception e) {
            logger.error("Error fetching task", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get tasks assigned to current user
     */
    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<Task>>> getMyTasks(Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);
            return ResponseEntity
                    .ok(ApiResponse.success("My tasks retrieved successfully", taskService.getMyTasks(user)));
        } catch (Exception e) {
            logger.error("Error fetching my tasks", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get tasks by status
     */
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByStatus(@PathVariable String status) {
        try {
            Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
            return ResponseEntity
                    .ok(ApiResponse.success("Tasks retrieved successfully", taskService.getTasksByStatus(taskStatus)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status"));
        } catch (Exception e) {
            logger.error("Error fetching tasks by status", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get tasks by project
     */
    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByProject(@PathVariable Long projectId) {
        try {
            return ResponseEntity
                    .ok(ApiResponse.success("Tasks retrieved successfully", taskService.getTasksByProject(projectId)));
        } catch (Exception e) {
            logger.error("Error fetching tasks by project", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get tasks for a specific lead
     */
    @GetMapping("/by-lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<Task>>> getTasksByLead(@PathVariable Long leadId) {
        try {
            return ResponseEntity
                    .ok(ApiResponse.success("Tasks retrieved successfully", taskService.getTasksByLead(leadId)));
        } catch (Exception e) {
            logger.error("Error fetching tasks by lead", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Get assignment history for a task
     */
    @GetMapping("/{id}/assignment-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<TaskAssignmentHistory>>> getAssignmentHistory(
            @PathVariable Long id, Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);

            // Check view permission
            if (!authService.canViewTask(id, auth, user.getId())) {
                logger.warn("User {} attempted to view history for task {} without permission",
                        user.getEmail(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
            }

            List<TaskAssignmentHistory> history = taskService.getAssignmentHistory(id);
            return ResponseEntity.ok(ApiResponse.success("Assignment history retrieved successfully", history));
        } catch (Exception e) {
            logger.error("Error fetching assignment history", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Create new task - NOW OPEN TO ALL USERS (per requirement)
     * 
     * IMPORTANT: Due date is MANDATORY (as of V10 migration)
     * - Validation occurs at entity level (@NotNull annotation)
     * - Missing due_date will return 400 Bad Request
     * - Business rule: due_date must be >= task creation date
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ✅ Changed from ADMIN-only
    public ResponseEntity<ApiResponse<Task>> createTask(@jakarta.validation.Valid @RequestBody Task task,
            Authentication auth) {
        try {
            PortalUser createdBy = getCurrentUser(auth);

            logger.info("User {} creating task: {} (Due: {})",
                    createdBy.getEmail(), task.getTitle(), task.getDueDate());

            Task createdTask = taskService.createTask(task, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Task created successfully", createdTask));
        } catch (Exception e) {
            logger.error("Error creating task", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Update task with RBAC enforcement
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Task>> updateTask(
            @PathVariable Long id,
            @RequestBody Task task,
            Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);

            // Authorization check happens in service layer
            Task updatedTask = taskService.updateTask(id, task, auth, user.getId());

            logger.info("User {} updated task {}", user.getEmail(), id);
            return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updatedTask));

        } catch (AccessDeniedException e) {
            logger.warn("Access denied for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error updating task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating task", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Assign/reassign task - NOW OPEN TO ALL USERS (per requirement)
     * Assignment history is automatically recorded
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ✅ Changed from ADMIN-only
    public ResponseEntity<ApiResponse<Task>> assignTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            PortalUser assignedBy = getCurrentUser(auth);

            // Extract parameters
            Long userId = request.get("userId") != null
                    ? Long.valueOf(request.get("userId").toString())
                    : null;
            String notes = request.get("notes") != null
                    ? request.get("notes").toString()
                    : null;

            Task assignedTask = taskService.assignTask(id, userId, assignedBy.getId(), notes);

            logger.info("User {} assigned task {} to user {}",
                    assignedBy.getEmail(), id, userId);

            return ResponseEntity.ok(ApiResponse.success("Task assigned successfully", assignedTask));

        } catch (RuntimeException e) {
            logger.error("Error assigning task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error assigning task", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Delete task with RBAC enforcement
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ✅ Changed from ADMIN-only
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id, Authentication auth) {
        try {
            PortalUser user = getCurrentUser(auth);

            // Authorization check happens in service layer
            taskService.deleteTask(id, auth, user.getId());

            logger.info("User {} deleted task {}", user.getEmail(), id);
            return ResponseEntity.ok(ApiResponse.success("Task deleted successfully"));

        } catch (AccessDeniedException e) {
            logger.warn("Access denied for delete: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error deleting task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting task", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Internal server error"));
        }
    }

    /**
     * Manual trigger for alert system (Testing/Admin)
     * Forces immediate deadline check regardless of schedule
     */
    @PostMapping("/alerts/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTaskAlerts() {
        logger.info("Manual task alert trigger requested");

        try {
            taskAlertScheduler.triggerManualCheck();
            return ResponseEntity.ok(ApiResponse.success("Task deadline alert check completed successfully", Map.of(
                    "timestamp", java.time.LocalDateTime.now())));
        } catch (Exception e) {
            logger.error("Error during manual alert trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ===== Helper Methods =====

    private PortalUser getCurrentUser(Authentication auth) {
        return portalUserRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
