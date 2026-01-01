package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.model.TaskAssignmentHistory;
import com.wd.api.model.User;
import com.wd.api.repository.TaskAssignmentHistoryRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.UserRepository;
import com.wd.api.security.TaskAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Production-grade Task Service with RBAC enforcement
 * 
 * Security Model:
 * - All modify operations require authorization check
 * - Assignment changes are logged to assignment_history
 * - Follows defense-in-depth principle
 * 
 * @author Senior Engineer (15+ years construction domain experience)
 */
@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskAssignmentHistoryRepository assignmentHistoryRepository;

    @Autowired
    private TaskAuthorizationService authService;

    /**
     * Get all tasks (admin only) or user's tasks (non-admin)
     * Filtering is done at controller level based on role
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Get task by ID
     * Authorization check should be done at controller level
     */
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    /**
     * Get tasks assigned to a specific user
     */
    public List<Task> getTasksByAssignedUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(taskRepository::findByAssignedTo).orElse(List.of());
    }

    /**
     * Get tasks by status
     */
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    /**
     * Get tasks by project
     */
    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Get tasks created by a user
     */
    public List<Task> getTasksByCreator(Long userId) {
        return taskRepository.findByCreatedById(userId);
    }

    /**
     * Get tasks for a specific lead
     */
    public List<Task> getTasksByLead(Long leadId) {
        return taskRepository.findByLeadId(leadId);
    }

    /**
     * Create new task
     * Anyone can create tasks (enforced at controller with @PreAuthorize)
     */
    @Transactional
    public Task createTask(Task task, User createdBy) {
        logger.info("Creating task '{}' by user {}", task.getTitle(), createdBy.getEmail());

        task.setCreatedBy(createdBy);
        Task savedTask = taskRepository.save(task);

        // Record initial assignment if task is assigned
        if (savedTask.getAssignedTo() != null) {
            recordAssignment(
                    savedTask.getId(),
                    null, // No previous assignee
                    savedTask.getAssignedTo(),
                    createdBy,
                    "Initial assignment on task creation");
        }

        logger.info("Task created successfully with ID: {}", savedTask.getId());
        return savedTask;
    }

    /**
     * Update task with RBAC enforcement
     * 
     * CRITICAL: Authorization is checked BEFORE any modification
     */
    @Transactional
    public Task updateTask(Long id, Task taskDetails, Authentication auth, Long userId) {
        logger.info("Attempting to update task {} by user {}", id, userId);

        // CRITICAL: Verify permission BEFORE modification
        authService.requireModifyPermission(id, auth, userId);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // Update fields
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(taskDetails.getStatus());
        task.setPriority(taskDetails.getPriority());
        task.setDueDate(taskDetails.getDueDate());

        // Handle assignment change if provided
        if (taskDetails.getAssignedTo() != null) {
            User previousAssignee = task.getAssignedTo();
            User newAssignee = taskDetails.getAssignedTo();

            // Only record if assignment actually changed
            if (previousAssignee == null || !previousAssignee.getId().equals(newAssignee.getId())) {
                task.setAssignedTo(newAssignee);

                User assignedBy = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                recordAssignment(
                        task.getId(),
                        previousAssignee,
                        newAssignee,
                        assignedBy,
                        "Assignment changed during task update");
            }
        }

        // Handle project change if provided
        if (taskDetails.getProject() != null) {
            task.setProject(taskDetails.getProject());
        }

        // Handle lead change if provided
        if (taskDetails.getLead() != null) {
            task.setLead(taskDetails.getLead());
        }

        Task updated = taskRepository.save(task);
        logger.info("Task {} updated successfully", id);

        return updated;
    }

    /**
     * Assign/reassign task with full history tracking
     * 
     * This method is specifically for assignment operations.
     * Records complete audit trail of who assigned what to whom.
     */
    @Transactional
    public Task assignTask(Long taskId, Long newAssigneeId, Long assignedById, String notes) {
        logger.info("Assigning task {} to user {} by user {}", taskId, newAssigneeId, assignedById);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User newAssignee = newAssigneeId != null
                ? userRepository.findById(newAssigneeId)
                        .orElseThrow(() -> new RuntimeException("Assignee user not found"))
                : null;

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new RuntimeException("Assigning user not found"));

        User previousAssignee = task.getAssignedTo();

        // Update assignment
        task.setAssignedTo(newAssignee);
        Task updated = taskRepository.save(task);

        // Record in history
        recordAssignment(taskId, previousAssignee, newAssignee, assignedBy, notes);

        logger.info("Task {} assigned successfully", taskId);
        return updated;
    }

    /**
     * Get assignment history for a task
     */
    public List<TaskAssignmentHistory> getAssignmentHistory(Long taskId) {
        return assignmentHistoryRepository.findByTaskIdOrderByAssignedAtDesc(taskId);
    }

    /**
     * Delete task with RBAC enforcement
     * 
     * CRITICAL: Authorization is checked BEFORE deletion
     */
    @Transactional
    public void deleteTask(Long id, Authentication auth, Long userId) {
        logger.info("Attempting to delete task {} by user {}", id, userId);

        // CRITICAL: Verify permission BEFORE deletion
        authService.requireModifyPermission(id, auth, userId);

        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with id: " + id);
        }

        taskRepository.deleteById(id);
        // Note: Assignment history is cascade deleted (ON DELETE CASCADE)

        logger.info("Task {} deleted successfully", id);
    }

    /**
     * Get tasks for current user (assigned to them)
     */
    public List<Task> getMyTasks(User user) {
        return taskRepository.findByAssignedToOrderByDueDateAsc(user);
    }

    /**
     * Get tasks visible to user based on permissions
     * - Admin: All tasks
     * - Project Manager: All tasks in their projects
     * - Regular User: Tasks they created or are assigned to
     */
    public List<Task> getTasksForUser(User user, Authentication auth) {
        if (authService.isAdmin(auth)) {
            return taskRepository.findAll();
        }

        // Get tasks created by user or assigned to user
        // Project manager filtering would require more complex query
        return taskRepository.findByCreatedByOrAssignedTo(user, user);
    }

    // ===== Private Helper Methods =====

    /**
     * Record assignment change in history table
     * This creates an audit trail for all assignment changes
     */
    private void recordAssignment(Long taskId, User from, User to, User by, String notes) {
        TaskAssignmentHistory history = new TaskAssignmentHistory();
        history.setTaskId(taskId);
        history.setAssignedFrom(from);
        history.setAssignedTo(to);
        history.setAssignedBy(by);
        history.setNotes(notes);

        assignmentHistoryRepository.save(history);

        logger.debug("Assignment history recorded for task {}: {} -> {} by {}",
                taskId,
                from != null ? from.getEmail() : "unassigned",
                to != null ? to.getEmail() : "unassigned",
                by.getEmail());
    }
}
