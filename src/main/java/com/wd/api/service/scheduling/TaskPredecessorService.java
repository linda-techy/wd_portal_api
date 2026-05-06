package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the multi-predecessor join table {@code task_predecessor}.
 *
 * <p>S2 dropped the legacy {@code Task.dependsOnTaskId} column;
 * {@code task_predecessor} is the canonical edge store from this point
 * on. CPM denormalized columns are kept consistent via a recompute hook
 * after every mutation.
 */
@Service
public class TaskPredecessorService {

    private final TaskPredecessorRepository predecessorRepo;
    private final TaskRepository taskRepo;
    private final CpmService cpmService;

    public TaskPredecessorService(TaskPredecessorRepository predecessorRepo,
                                  TaskRepository taskRepo,
                                  CpmService cpmService) {
        this.predecessorRepo = predecessorRepo;
        this.taskRepo = taskRepo;
        this.cpmService = cpmService;
    }

    /** A predecessor entry as supplied by the controller — id + lag in days. */
    public record PredecessorEntry(Long predecessorId, Integer lagDays) {
        public PredecessorEntry {
            Objects.requireNonNull(predecessorId, "predecessorId");
            if (lagDays == null) lagDays = 0;
        }
    }

    /**
     * Replace-all semantics: deletes every existing predecessor of {@code successorId}
     * and inserts the supplied list. Validates each new edge against cycles before
     * any DB writes.
     */
    @Transactional
    public List<TaskPredecessor> replacePredecessors(Long successorId, List<PredecessorEntry> entries) {
        Objects.requireNonNull(successorId, "successorId");
        List<PredecessorEntry> safe = entries == null ? List.of() : entries;

        // 1) Validate each new edge against the *existing* graph.
        for (PredecessorEntry e : safe) {
            TaskGraphValidator.assertNoCycle(
                    successorId,
                    e.predecessorId(),
                    this::predecessorsOf);
        }

        // 2) Replace.
        predecessorRepo.deleteBySuccessorId(successorId);
        List<TaskPredecessor> saved = new ArrayList<>(safe.size());
        for (PredecessorEntry e : safe) {
            TaskPredecessor row = new TaskPredecessor(successorId, e.predecessorId(), e.lagDays());
            saved.add(predecessorRepo.save(row));
        }

        // S2 PR1: keep CPM denormalized columns consistent on every graph mutation.
        Task t = taskRepo.findById(successorId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + successorId));
        if (t.getProject() != null && t.getProject().getId() != null) {
            cpmService.recompute(t.getProject().getId());
        }

        return saved;
    }

    private List<Long> predecessorsOf(Long taskId) {
        return predecessorRepo.findBySuccessorId(taskId).stream()
                .map(TaskPredecessor::getPredecessorId)
                .toList();
    }
}
