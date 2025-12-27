package com.wd.api.security;

import com.wd.api.model.CustomerProject;
import com.wd.api.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Production-grade Task Authorization Service
 * 
 * Implements role-based + ownership-based + project-level access control.
 * Follows SOLID principles and defense-in-depth security.
 * 
 * Authorization Hierarchy:
 * 1. ADMIN - Can do everything
 * 2. Project Manager - Can edit all tasks in their projects
 * 3. Task Creator - Can edit/delete their own tasks
 * 4. Regular User - Can create tasks, view assigned tasks
 * 
 * @author Senior Engineer (15+ years experience)
 */
@Service
public class TaskAuthorizationService {

    @Autowired
    private TaskRepository taskRepository;

    /**
     * Check if authenticated user has ADMIN role
     */
    public boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if user is the creator of a task
     */
    public boolean isTaskCreator(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }

        return taskRepository.findById(taskId)
                .map(task -> task.getCreatedBy() != null &&
                        task.getCreatedBy().getId().equals(userId))
                .orElse(false);
    }

    /**
     * Check if user is project manager of the task's project
     * 
     * Project managers have full control over all tasks in their projects,
     * regardless of who created them.
     */
    public boolean isProjectManager(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }

        return taskRepository.findById(taskId)
                .map(task -> {
                    CustomerProject project = task.getProject();
                    if (project == null || project.getProjectManagerId() == null) {
                        return false;
                    }
                    return project.getProjectManagerId().equals(userId);
                })
                .orElse(false);
    }

    /**
     * Comprehensive permission check: Can user modify this task?
     * 
     * Returns TRUE if user:
     * - Is an ADMIN (can modify all tasks), OR
     * - Is the project manager (can modify all project tasks), OR
     * - Is the task creator (can modify own tasks)
     * 
     * This implements the "principle of least privilege" - users only get
     * the minimum permissions needed to do their job.
     */
    public boolean canModifyTask(Long taskId, Authentication auth, Long userId) {
        if (taskId == null || auth == null || userId == null) {
            return false;
        }

        // Level 1: Admin has full access
        if (isAdmin(auth)) {
            return true;
        }

        // Level 2: Project manager can modify all tasks in their projects
        if (isProjectManager(taskId, userId)) {
            return true;
        }

        // Level 3: Task creator can modify their own tasks
        if (isTaskCreator(taskId, userId)) {
            return true;
        }

        // No match - deny access
        return false;
    }

    /**
     * Validate and throw exception if modification is not allowed
     * 
     * Use this in service methods to enforce authorization before
     * performing any database modifications.
     * 
     * @throws AccessDeniedException if user doesn't have permission
     */
    public void requireModifyPermission(Long taskId, Authentication auth, Long userId) {
        if (!canModifyTask(taskId, auth, userId)) {
            throw new AccessDeniedException(
                    "Access denied: You don't have permission to modify this task. " +
                            "Only admins, project managers, or task creators can modify tasks.");
        }
    }

    /**
     * Check if user can view a task
     * 
     * Users can view tasks if:
     * - They are admin (see all tasks)
     * - They are project manager (see all project tasks)
     * - They created the task
     * - They are assigned to the task
     */
    public boolean canViewTask(Long taskId, Authentication auth, Long userId) {
        if (taskId == null || auth == null || userId == null) {
            return false;
        }

        // Admins can view all
        if (isAdmin(auth)) {
            return true;
        }

        return taskRepository.findById(taskId)
                .map(task -> {
                    // Project manager can view all project tasks
                    if (task.getProject() != null &&
                            task.getProject().getProjectManagerId() != null &&
                            task.getProject().getProjectManagerId().equals(userId)) {
                        return true;
                    }

                    // Creator can view their own tasks
                    if (task.getCreatedBy() != null &&
                            task.getCreatedBy().getId().equals(userId)) {
                        return true;
                    }

                    // Assigned user can view their tasks
                    if (task.getAssignedTo() != null &&
                            task.getAssignedTo().getId().equals(userId)) {
                        return true;
                    }

                    return false;
                })
                .orElse(false);
    }

    /**
     * Get permission summary for a task (used in DTOs for UI control)
     */
    public TaskPermissions getTaskPermissions(Long taskId, Authentication auth, Long userId) {
        boolean canEdit = canModifyTask(taskId, auth, userId);
        boolean canDelete = canModifyTask(taskId, auth, userId);
        boolean canView = canViewTask(taskId, auth, userId);
        boolean isAdmin = isAdmin(auth);

        return new TaskPermissions(canView, canEdit, canDelete, isAdmin);
    }

    /**
     * Simple DTO to encapsulate task permissions
     */
    public static class TaskPermissions {
        private final boolean canView;
        private final boolean canEdit;
        private final boolean canDelete;
        private final boolean isAdmin;

        public TaskPermissions(boolean canView, boolean canEdit, boolean canDelete, boolean isAdmin) {
            this.canView = canView;
            this.canEdit = canEdit;
            this.canDelete = canDelete;
            this.isAdmin = isAdmin;
        }

        public boolean canView() {
            return canView;
        }

        public boolean canEdit() {
            return canEdit;
        }

        public boolean canDelete() {
            return canDelete;
        }

        public boolean isAdmin() {
            return isAdmin;
        }
    }
}
