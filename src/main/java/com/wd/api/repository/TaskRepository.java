package com.wd.api.repository;

import com.wd.api.model.Task;
import com.wd.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

        List<Task> findByAssignedTo(User assignedTo);

        List<Task> findByStatus(Task.TaskStatus status);

        List<Task> findByProjectId(Long projectId);

        List<Task> findByAssignedToOrderByDueDateAsc(User assignedTo);

        // New methods for RBAC
        List<Task> findByCreatedById(Long createdById);

        List<Task> findByCreatedByOrAssignedTo(User createdBy, User assignedTo);

        List<Task> findByLeadId(Long leadId);

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
}
