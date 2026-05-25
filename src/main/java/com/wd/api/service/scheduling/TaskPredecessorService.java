package com.wd.api.service.scheduling;

import com.wd.api.dto.scheduling.PredecessorEdgeDto;
import com.wd.api.model.enums.DependencyType;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @PersistenceContext
    private EntityManager em;

    public TaskPredecessorService(TaskPredecessorRepository predecessorRepo,
                                  TaskRepository taskRepo,
                                  CpmService cpmService) {
        this.predecessorRepo = predecessorRepo;
        this.taskRepo = taskRepo;
        this.cpmService = cpmService;
    }

    /** A predecessor entry as supplied by the controller — id, lag in days, and dependency type. */
    public record PredecessorEntry(Long predecessorId, Integer lagDays, DependencyType depType) {
        public PredecessorEntry {
            Objects.requireNonNull(predecessorId, "predecessorId");
            if (lagDays == null) lagDays = 0;
            if (depType == null) depType = DependencyType.FS;
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
        //
        // Force-flush after the delete so the @SQLDelete UPDATEs (which set
        // deleted_at = NOW()) hit the DB BEFORE we issue INSERTs for the new
        // rows. Hibernate's default action queue orders INSERTs BEFORE UPDATEs
        // at flush time — without this explicit flush, the new row's INSERT
        // races the still-live old row and collides with the partial-unique
        // index uq_task_predecessor_pair_live (see V154).
        predecessorRepo.deleteBySuccessorId(successorId);
        em.flush();
        List<TaskPredecessor> saved = new ArrayList<>(safe.size());
        for (PredecessorEntry e : safe) {
            TaskPredecessor row = new TaskPredecessor(successorId, e.predecessorId(), e.lagDays());
            row.setDepType(e.depType());
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

    /** Current predecessor edges for a task, eager-loaded with predecessor titles. */
    @Transactional(readOnly = true)
    public List<PredecessorEdgeDto> listPredecessors(Long successorId) {
        Objects.requireNonNull(successorId, "successorId");
        List<TaskPredecessor> edges = predecessorRepo.findBySuccessorId(successorId);
        if (edges.isEmpty()) return List.of();

        // Single-shot fetch of predecessor titles to avoid N+1.
        List<Long> predIds = edges.stream().map(TaskPredecessor::getPredecessorId).toList();
        Map<Long, String> titleById = new HashMap<>();
        taskRepo.findAllById(predIds).forEach(t -> titleById.put(t.getId(), t.getTitle()));

        List<PredecessorEdgeDto> out = new ArrayList<>(edges.size());
        for (TaskPredecessor e : edges) {
            out.add(new PredecessorEdgeDto(
                    e.getId(),
                    e.getSuccessorId(),
                    e.getPredecessorId(),
                    titleById.getOrDefault(e.getPredecessorId(), "(deleted)"),
                    e.getLagDays(),
                    e.getDepType() != null ? e.getDepType().name() : "FS"
            ));
        }
        return out;
    }

    private List<Long> predecessorsOf(Long taskId) {
        return predecessorRepo.findBySuccessorId(taskId).stream()
                .map(TaskPredecessor::getPredecessorId)
                .toList();
    }
}
