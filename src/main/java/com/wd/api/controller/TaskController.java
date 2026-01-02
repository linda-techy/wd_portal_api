package com.wd.api.controller;

import com.wd.api.model.Task;
import com.wd.api.model.TaskAssignmentHistory;
import com.wd.api.model.User;
import com.wd.api.repository.UserRepository;
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
    private UserRepository userRepository;

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
    public ResponseEntity<?> getAlertStats(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(taskAlertService.getAlertStats(days));
    }

    @GetMapping("/alerts/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<com.wd.api.model.TaskAlert>> getRecentAlerts() {
        return ResponseEntity.ok(taskAlertService.getRecentAlerts());
    }

    /**
     * Get all tasks - admins see all, users see only theirs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getAllTasks(Authentication auth) {
        User user = getCurrentUser(auth);

        List<Task> tasks;
        if (authService.isAdmin(auth)) {
            tasks = taskService.getAllTasks();
            logger.info("Admin {} retrieved all tasks", user.getEmail());
        } else {
            tasks = taskService.getTasksForUser(user, auth);
            logger.info("User {} retrieved their tasks", user.getEmail());
        }

        return ResponseEntity.ok(tasks);
    }

    /**
     * Get task by ID with permission check
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);

        Optional<Task> task = taskService.getTaskById(id);
        if (task.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check view permission
        if (!authService.canViewTask(id, auth, user.getId())) {
            logger.warn("User {} attempted to view task {} without permission", user.getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(task.get());
    }

    /**
     * Get tasks assigned to current user
     */
    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getMyTasks(Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(taskService.getMyTasks(user));
    }

    /**
     * Get tasks by status
     */
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable String status) {
        try {
            Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(taskService.getTasksByStatus(taskStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get tasks by project
     */
    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    /**
     * Get tasks for a specific lead
     */
    @GetMapping("/by-lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getTasksByLead(@PathVariable Long leadId) {
        return ResponseEntity.ok(taskService.getTasksByLead(leadId));
    }

    /**
     * Get assignment history for a task
     */
    @GetMapping("/{id}/assignment-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TaskAssignmentHistory>> getAssignmentHistory(
            @PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);

        // Check view permission
        if (!authService.canViewTask(id, auth, user.getId())) {
            logger.warn("User {} attempted to view history for task {} without permission",
                    user.getEmail(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TaskAssignmentHistory> history = taskService.getAssignmentHistory(id);
        return ResponseEntity.ok(history);
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
    public ResponseEntity<Task> createTask(@jakarta.validation.Valid @RequestBody Task task, Authentication auth) {
        User createdBy = getCurrentUser(auth);

        logger.info("User {} creating task: {} (Due: {})",
                createdBy.getEmail(), task.getTitle(), task.getDueDate());

        Task createdTask = taskService.createTask(task, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * Update task with RBAC enforcement
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestBody Task task,
            Authentication auth) {
        try {
            User user = getCurrentUser(auth);

            // Authorization check happens in service layer
            Task updatedTask = taskService.updateTask(id, task, auth, user.getId());

            logger.info("User {} updated task {}", user.getEmail(), id);
            return ResponseEntity.ok(updatedTask);

        } catch (AccessDeniedException e) {
            logger.warn("Access denied for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error updating task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Assign/reassign task - NOW OPEN TO ALL USERS (per requirement)
     * Assignment history is automatically recorded
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ✅ Changed from ADMIN-only
    public ResponseEntity<?> assignTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            User assignedBy = getCurrentUser(auth);

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

            return ResponseEntity.ok(assignedTask);

        } catch (RuntimeException e) {
            logger.error("Error assigning task", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete task with RBAC enforcement
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // ✅ Changed from ADMIN-only
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth) {
        try {
            User user = getCurrentUser(auth);

            // Authorization check happens in service layer
            taskService.deleteTask(id, auth, user.getId());

            logger.info("User {} deleted task {}", user.getEmail(), id);
            return ResponseEntity.noContent().build();

        } catch (AccessDeniedException e) {
            logger.warn("Access denied for delete: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error deleting task", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manual trigger for alert system (Testing/Admin)
     * Forces immediate deadline check regardless of schedule
     */
    @PostMapping("/alerts/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerTaskAlerts() {
        logger.info("Manual task alert trigger requested");

        try {
            taskAlertScheduler.triggerManualCheck();
            return ResponseEntity.ok(Map.of(
                    "message", "Task deadline alert check completed successfully",
                    "timestamp", java.time.LocalDateTime.now()));
        } catch (Exception e) {
            logger.error("Error during manual alert trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===== Helper Methods =====

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
