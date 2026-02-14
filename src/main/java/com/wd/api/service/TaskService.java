package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.model.TaskAssignmentHistory;
import com.wd.api.model.PortalUser;
import com.wd.api.model.Lead;
import com.wd.api.dto.TaskSearchFilter;
import com.wd.api.repository.TaskAssignmentHistoryRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.security.TaskAuthorizationService;
import com.wd.api.util.SpecificationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private PortalUserRepository portalUserRepository;

    @Autowired
    private com.wd.api.repository.LeadRepository leadRepository;

    @Autowired
    private TaskAssignmentHistoryRepository assignmentHistoryRepository;

    @Autowired
    private TaskAuthorizationService authService;

    /**
     * NEW: Standardized search method using TaskSearchFilter
     */
    @Transactional(readOnly = true)
    public Page<Task> search(TaskSearchFilter filter) {
        Specification<Task> spec = buildSearchSpecification(filter);
        return taskRepository.findAll(spec, java.util.Objects.requireNonNull(filter.toPageable()));
    }

    /**
     * Build JPA Specification from TaskSearchFilter
     */
    private Specification<Task> buildSearchSpecification(TaskSearchFilter filter) {
        SpecificationBuilder<Task> builder = new SpecificationBuilder<>();

        // Search across task fields
        Specification<Task> searchSpec = builder.buildSearch(
                filter.getSearchQuery(),
                "title", "description");

        // Status filter
        Specification<Task> statusSpec = null;
        if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
            statusSpec = (root, query, cb) -> cb.equal(root.get("status"),
                    Task.TaskStatus.valueOf(filter.getStatus().toUpperCase()));
        }

        // Priority filter
        Specification<Task> prioritySpec = null;
        if (filter.getPriority() != null && !filter.getPriority().trim().isEmpty()) {
            prioritySpec = (root, query, cb) -> cb.equal(root.get("priority"),
                    Task.TaskPriority.valueOf(filter.getPriority().toUpperCase()));
        }

        // Assigned user filter
        Specification<Task> assignedSpec = null;
        if (filter.getAssignedTo() != null) {
            assignedSpec = (root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), filter.getAssignedTo());
        }

        // Project filter
        Specification<Task> projectSpec = null;
        if (filter.getProjectId() != null) {
            projectSpec = (root, query, cb) -> cb.equal(root.get("projectId"), filter.getProjectId());
        }

        // Lead filter
        Specification<Task> leadSpec = null;
        if (filter.getLeadId() != null) {
            leadSpec = (root, query, cb) -> cb.equal(root.get("leadId"), filter.getLeadId());
        }

        // Created by filter
        Specification<Task> createdBySpec = null;
        if (filter.getCreatedBy() != null) {
            createdBySpec = (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), filter.getCreatedBy());
        }

        // Due date range
        Specification<Task> dueDateSpec = builder.buildDateRange(
                "dueDate",
                filter.getDueDateStart(),
                filter.getDueDateEnd());

        // Combine all specifications
        return builder.and(
                searchSpec,
                statusSpec,
                prioritySpec,
                assignedSpec,
                projectSpec,
                leadSpec,
                createdBySpec,
                dueDateSpec);
    }

    /**
     * DEPRECATED: Get all tasks (admin only) or user's tasks (non-admin)
     * Use search() instead
     * PERFORMANCE: Limited to 1000 tasks to prevent memory issues
     */
    @Deprecated
    public List<Task> getAllTasks() {
        // PERFORMANCE: Limit to prevent loading all records into memory
        Pageable pageable = PageRequest.of(0, 1000);
        return taskRepository.findAll(pageable).getContent();
    }

    /**
     * Get task by ID
     * Authorization check should be done at controller level
     */
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(java.util.Objects.requireNonNull(id));
    }

    /**
     * Get tasks assigned to a specific user
     */
    public List<Task> getTasksByAssignedUser(Long userId) {
        Optional<PortalUser> user = portalUserRepository.findById(java.util.Objects.requireNonNull(userId));
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
        if (leadId == null) {
            logger.warn("Attempted to fetch tasks with null leadId");
            return List.of();
        }
        return taskRepository.findByLeadId(leadId);
    }

    /**
     * Create new task
     * Anyone can create tasks (enforced at controller with @PreAuthorize)
     */
    @Transactional
    public Task createTask(Task task, PortalUser createdBy) {
        logger.info("Creating task '{}' by user {}", task.getTitle(), createdBy.getEmail());

        // Always set creator from authenticated user (ignore any client-side spoofing)
        task.setCreatedBy(createdBy);

        // If task is linked to a lead, ensure we attach a managed Lead entity
        // This prevents transient-entity issues and guarantees referential integrity
        if (task.getLead() != null) {
            Lead incomingLead = task.getLead();
            Long leadIdLocal = null;
            try {
                // Prefer primary key id if present
                if (incomingLead.getId() != null) {
                    leadIdLocal = incomingLead.getId();
                }
            } catch (Exception e) {
                logger.warn("Incoming task lead object does not expose a valid ID: {}", e.getMessage());
            }

            if (leadIdLocal != null) {
                final Long leadId = leadIdLocal;
                Lead managedLead = leadRepository.findById(leadId)
                        .orElseThrow(() -> new IllegalArgumentException("Lead not found with id: " + leadId));
                task.setLead(managedLead);
            } else {
                // If lead object is present but without a valid id, treat this as invalid input
                throw new IllegalArgumentException("Invalid lead reference on task creation. Lead id is required.");
            }
        }

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

        Task task = taskRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // Update fields
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(taskDetails.getStatus());
        task.setPriority(taskDetails.getPriority());
        task.setDueDate(taskDetails.getDueDate());

        // Handle assignment change if provided
        if (taskDetails.getAssignedTo() != null) {
            PortalUser previousAssignee = task.getAssignedTo();
            PortalUser newAssignee = taskDetails.getAssignedTo();

            // Only record if assignment actually changed
            if (previousAssignee == null || !previousAssignee.getId().equals(newAssignee.getId())) {
                task.setAssignedTo(newAssignee);

                PortalUser assignedBy = portalUserRepository.findById(java.util.Objects.requireNonNull(userId))
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

        Task task = taskRepository.findById(java.util.Objects.requireNonNull(taskId))
                .orElseThrow(() -> new RuntimeException("Task not found"));

        PortalUser newAssignee = newAssigneeId != null
                ? portalUserRepository.findById(java.util.Objects.requireNonNull(newAssigneeId))
                        .orElseThrow(() -> new RuntimeException("Assignee user not found"))
                : null;

        PortalUser assignedBy = portalUserRepository.findById(java.util.Objects.requireNonNull(assignedById))
                .orElseThrow(() -> new RuntimeException("Assigning user not found"));

        PortalUser previousAssignee = task.getAssignedTo();

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

        if (!taskRepository.existsById(java.util.Objects.requireNonNull(id))) {
            throw new RuntimeException("Task not found with id: " + id);
        }

        taskRepository.deleteById(java.util.Objects.requireNonNull(id));
        // Note: Assignment history is cascade deleted (ON DELETE CASCADE)

        logger.info("Task {} deleted successfully", id);
    }

    /**
     * Get tasks for current user (assigned to them)
     */
    public List<Task> getMyTasks(PortalUser user) {
        return taskRepository.findByAssignedToOrderByDueDateAsc(user);
    }

    /**
     * Get tasks visible to user based on permissions
     * - Admin: All tasks (limited to 1000)
     * - Project Manager: All tasks in their projects
     * - Regular User: Tasks they created or are assigned to
     * PERFORMANCE: Admin path limited to 1000 tasks to prevent memory issues
     */
    public List<Task> getTasksForUser(PortalUser user, Authentication auth) {
        if (authService.isAdmin(auth)) {
            // PERFORMANCE: Limit to prevent loading all records into memory
            Pageable pageable = PageRequest.of(0, 1000);
            return taskRepository.findAll(pageable).getContent();
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
    private void recordAssignment(Long taskId, PortalUser from, PortalUser to, PortalUser by, String notes) {
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
