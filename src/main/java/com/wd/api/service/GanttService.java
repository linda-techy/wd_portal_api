package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.TaskPredecessorRepository;
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
 * - S2 PR2 dropped the legacy single-predecessor column. Predecessor
 *   edits now flow through {@code PUT /tasks/{id}/predecessors}
 *   (added in S1) — this service no longer touches the predecessor
 *   graph. Cycle detection lives in
 *   {@link com.wd.api.service.scheduling.TaskGraphValidator}.
 */
@Service
public class GanttService {

    private static final Logger logger = LoggerFactory.getLogger(GanttService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskPredecessorRepository taskPredecessorRepository;

    @Autowired
    private com.wd.api.service.scheduling.CpmService cpmService;

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

        // Single-pass predecessor lookup so per-task DTOs don't re-query.
        Map<Long, List<Long>> predecessorIdsByTaskId = new HashMap<>();
        if (!tasks.isEmpty()) {
            List<Long> taskIds = tasks.stream().map(Task::getId).toList();
            for (TaskPredecessor tp : taskPredecessorRepository.findBySuccessorIdIn(taskIds)) {
                predecessorIdsByTaskId
                        .computeIfAbsent(tp.getSuccessorId(), k -> new ArrayList<>())
                        .add(tp.getPredecessorId());
            }
        }

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

            taskDtos.add(buildTaskDto(t, overdue,
                    predecessorIdsByTaskId.getOrDefault(t.getId(), List.of())));
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
     *
     * <p>Predecessor edits go through PUT /tasks/{id}/predecessors
     * (handled by TaskPredecessorService) — no longer accepted here.
     */
    @Transactional
    public Task updateTaskSchedule(Long taskId,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   Integer progressPercent) {

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

        task.setStartDate(startDate);
        task.setEndDate(endDate);
        if (progressPercent != null) task.setProgressPercent(progressPercent);

        Task saved = taskRepository.save(task);
        // S2 PR1: schedule mutations change CPM inputs; keep denormalized columns consistent.
        if (saved.getProject() != null && saved.getProject().getId() != null) {
            cpmService.recompute(saved.getProject().getId());
        }
        return saved;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildTaskDto(Task t, boolean overdue, List<Long> predecessorIds) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", t.getId());
        dto.put("title", t.getTitle());
        dto.put("status", t.getStatus());
        dto.put("priority", t.getPriority());
        dto.put("startDate", t.getStartDate());
        dto.put("endDate", t.getEndDate());
        dto.put("dueDate", t.getDueDate());
        dto.put("progressPercent", t.getProgressPercent() != null ? t.getProgressPercent() : 0);
        dto.put("predecessorIds", predecessorIds);
        dto.put("overdue", overdue);
        return dto;
    }
}
