package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for Gantt / project timeline data.
 *
 * Design notes:
 * - Portal API owns all mutations; customer API is read-only.
 * - Circular dependency detection uses iterative traversal (no recursion limit issues).
 */
@Service
public class GanttService {

    private static final Logger logger = LoggerFactory.getLogger(GanttService.class);

    @Autowired
    private TaskRepository taskRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // Read
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Build the full Gantt payload for a project.
     *
     * Response shape:
     * <pre>
     * {
     *   "tasks": [...],
     *   "projectStartDate": "2026-01-01",   // nullable
     *   "projectEndDate":   "2026-12-31",   // nullable
     *   "overallProgress":  42,             // average across non-cancelled tasks
     *   "overdueTasks":     3
     * }
     * </pre>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGanttData(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectIdOrderedForGantt(projectId);
        LocalDate today = LocalDate.now();

        LocalDate projectStart = null;
        LocalDate projectEnd = null;
        int totalProgress = 0;
        int countForProgress = 0;
        int overdueTasks = 0;

        List<Map<String, Object>> taskDtos = new ArrayList<>();

        for (Task t : tasks) {
            // Project date envelope
            if (t.getStartDate() != null) {
                if (projectStart == null || t.getStartDate().isBefore(projectStart)) {
                    projectStart = t.getStartDate();
                }
            }
            if (t.getEndDate() != null) {
                if (projectEnd == null || t.getEndDate().isAfter(projectEnd)) {
                    projectEnd = t.getEndDate();
                }
            }

            // Overdue: end_date < today AND not done
            boolean overdue = t.getEndDate() != null
                    && t.getEndDate().isBefore(today)
                    && t.getStatus() != Task.TaskStatus.COMPLETED
                    && t.getStatus() != Task.TaskStatus.CANCELLED;
            if (overdue) overdueTasks++;

            // Progress average (exclude cancelled)
            if (t.getStatus() != Task.TaskStatus.CANCELLED) {
                totalProgress += (t.getProgressPercent() != null ? t.getProgressPercent() : 0);
                countForProgress++;
            }

            taskDtos.add(buildTaskDto(t, overdue));
        }

        int overallProgress = countForProgress > 0 ? totalProgress / countForProgress : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tasks", taskDtos);
        result.put("projectStartDate", projectStart);
        result.put("projectEndDate", projectEnd);
        result.put("overallProgress", overallProgress);
        result.put("overdueTasks", overdueTasks);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Write
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Update scheduling fields for a single task.
     *
     * Validations:
     * 1. endDate >= startDate (when both are provided)
     * 2. progressPercent in [0, 100]
     * 3. No circular dependency in dependsOnTaskId chain
     */
    @Transactional
    public Task updateTaskSchedule(Long taskId,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   Integer progressPercent,
                                   Long dependsOnTaskId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Validate dates
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        // Validate progress
        if (progressPercent != null && (progressPercent < 0 || progressPercent > 100)) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }

        // Validate no circular dependency
        if (dependsOnTaskId != null) {
            if (dependsOnTaskId.equals(taskId)) {
                throw new IllegalArgumentException("A task cannot depend on itself");
            }
            detectCircularDependency(taskId, dependsOnTaskId);
        }

        task.setStartDate(startDate);
        task.setEndDate(endDate);
        if (progressPercent != null) task.setProgressPercent(progressPercent);
        task.setDependsOnTaskId(dependsOnTaskId);

        return taskRepository.save(task);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Walk the dependsOn chain from {@code candidateId} and ensure {@code taskId}
     * does not appear (which would form a cycle).
     */
    private void detectCircularDependency(Long taskId, Long candidateId) {
        Set<Long> visited = new HashSet<>();
        Long current = candidateId;

        while (current != null) {
            if (!visited.add(current)) break; // already visited — chain has its own cycle
            if (current.equals(taskId)) {
                throw new IllegalArgumentException(
                        "Circular dependency detected: task " + taskId + " is already in the dependency chain");
            }
            Task dep = taskRepository.findById(current).orElse(null);
            if (dep == null) break;
            current = dep.getDependsOnTaskId();
        }
    }

    private Map<String, Object> buildTaskDto(Task t, boolean overdue) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", t.getId());
        dto.put("title", t.getTitle());
        dto.put("status", t.getStatus());
        dto.put("priority", t.getPriority());
        dto.put("startDate", t.getStartDate());
        dto.put("endDate", t.getEndDate());
        dto.put("dueDate", t.getDueDate());
        dto.put("progressPercent", t.getProgressPercent() != null ? t.getProgressPercent() : 0);
        dto.put("dependsOnTaskId", t.getDependsOnTaskId());
        dto.put("overdue", overdue);
        return dto;
    }
}
