package com.wd.api.repository;

import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

        List<Task> findByAssignedTo(PortalUser assignedTo);

        List<Task> findByStatus(Task.TaskStatus status);

        List<Task> findByProjectId(Long projectId);

        List<Task> findByAssignedToOrderByDueDateAsc(PortalUser assignedTo);

        // New methods for RBAC
        List<Task> findByCreatedById(Long createdById);

        List<Task> findByCreatedByOrAssignedTo(PortalUser createdBy, PortalUser assignedTo);

        List<Task> findByLeadId(Long leadId);

        // Aggregation Queries
        int countByProjectId(Long projectId);

        int countByProjectIdAndStatus(Long projectId, String status);

        // ===== Task Deadline Alert Queries (V11 Alert System) =====
        // Note: These queries leverage indexes created in V10 migration

        /**
         * Find overdue tasks (for CRITICAL alerts)
         * Uses: idx_tasks_overdue index from V10
         */
        @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t " +
                        "WHERE t.dueDate < :date " +
                        "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
        List<Task> findOverdueTasks(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

        /**
         * Find tasks due on specific date (for HIGH alerts)
         * Uses: idx_tasks_overdue index from V10
         */
        @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t " +
                        "WHERE t.dueDate = :date " +
                        "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
        List<Task> findTasksDueOn(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

        /**
         * Find tasks due between dates (for MEDIUM alerts - 3-day warning)
         * Uses: idx_tasks_overdue index from V10
         */
        @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t " +
                        "WHERE t.dueDate BETWEEN :start AND :end " +
                        "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
        List<Task> findTasksDueBetween(
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

        /**
         * Count overdue tasks for a specific project
         * Uses: idx_tasks_project_due index from V10
         * 
         * @param projectId The project to check
         * @param date      Current date to compare against
         * @return Count of overdue tasks
         */
        @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Task t " +
                        "WHERE t.project.id = :projectId " +
                        "AND t.dueDate < :date " +
                        "AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
        int countOverdueByProjectId(
                        @org.springframework.data.repository.query.Param("projectId") Long projectId,
                        @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
