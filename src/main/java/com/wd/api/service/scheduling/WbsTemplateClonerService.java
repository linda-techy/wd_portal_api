package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.Task;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import com.wd.api.repository.ProjectMilestoneRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskPredecessorRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.service.scheduling.dto.WbsCloneResult;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Materialises a WbsTemplate into the project's WBS as a single-transaction
 * batch:
 *
 * <ul>
 *   <li>One {@link ProjectMilestone} per template phase.</li>
 *   <li>One {@link Task} per template task with floorLoop=NONE.</li>
 *   <li>For floorLoop=PER_FLOOR template tasks, one Task per floor index
 *       0..floorCount-1.</li>
 *   <li>{@link TaskPredecessor} edges replayed with cross-floor expansion:
 *     <ul>
 *       <li>PER_FLOOR → PER_FLOOR: same-floor link, one per floor.</li>
 *       <li>NONE → PER_FLOOR: fan-out to every floor instance.</li>
 *       <li>PER_FLOOR → NONE: top-floor (floorCount-1) is the source.</li>
 *       <li>NONE → NONE: single edge.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Snapshot semantics: no FK back to the template — all values are copied.
 */
@Service
public class WbsTemplateClonerService {

    private static final Logger log = LoggerFactory.getLogger(WbsTemplateClonerService.class);

    private final WbsTemplateRepository templates;
    private final WbsTemplatePhaseRepository phases;
    private final WbsTemplateTaskRepository templateTasks;
    private final WbsTemplateTaskPredecessorRepository templatePreds;
    private final ProjectMilestoneRepository milestones;
    private final TaskRepository taskRepo;
    private final TaskPredecessorRepository taskPredecessorRepo;

    public WbsTemplateClonerService(WbsTemplateRepository templates,
                                    WbsTemplatePhaseRepository phases,
                                    WbsTemplateTaskRepository templateTasks,
                                    WbsTemplateTaskPredecessorRepository templatePreds,
                                    ProjectMilestoneRepository milestones,
                                    TaskRepository taskRepo,
                                    TaskPredecessorRepository taskPredecessorRepo) {
        this.templates = templates;
        this.phases = phases;
        this.templateTasks = templateTasks;
        this.templatePreds = templatePreds;
        this.milestones = milestones;
        this.taskRepo = taskRepo;
        this.taskPredecessorRepo = taskPredecessorRepo;
    }

    @Transactional
    public WbsCloneResult cloneInto(CustomerProject project, Long templateId, Integer floorCount) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("project must be persisted");
        }
        // Single-shot guard: a second clone would silently double the WBS
        // (duplicate phases, tasks, predecessor edges) with no un-clone
        // available. Reject before doing any insert.
        if (milestones.existsByProjectId(project.getId())) {
            throw new IllegalStateException(
                    "Project " + project.getId() + " already has a WBS; clone is single-shot");
        }
        int floors = resolveFloorCount(project, floorCount);

        WbsTemplate template = templates.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("WbsTemplate " + templateId));

        List<WbsTemplatePhase> templatePhases =
                phases.findByTemplateIdOrderBySequenceAsc(template.getId());
        List<WbsTemplateTaskPredecessor> templateEdges =
                templatePreds.findAllForTemplate(template.getId());

        // (templateTaskId, floorIndex) → newTaskId. floorIndex = -1 for NONE.
        Map<TaskFloorKey, Long> taskIndex = new HashMap<>();

        int milestonesCreated = 0;
        int tasksCreated = 0;

        for (WbsTemplatePhase phase : templatePhases) {
            // roleHint is a role code (e.g. PROJECT_MANAGER, SCHEDULER), not a
            // user-facing description. Leave description null and let the
            // admin UI populate it explicitly.
            ProjectMilestone milestone = ProjectMilestone.builder()
                    .project(project)
                    .name(phase.getName())
                    .amount(BigDecimal.ZERO)
                    .status("PENDING")
                    .build();
            milestone = milestones.save(milestone);
            milestonesCreated++;

            List<WbsTemplateTask> phaseTasks =
                    templateTasks.findByPhaseIdOrderBySequenceAsc(phase.getId());
            for (WbsTemplateTask templateTask : phaseTasks) {
                if (templateTask.getFloorLoop() == FloorLoop.NONE) {
                    Task t = createTask(project, milestone, templateTask, -1);
                    t = taskRepo.save(t);
                    taskIndex.put(new TaskFloorKey(templateTask.getId(), -1), t.getId());
                    tasksCreated++;
                } else {
                    for (int floor = 0; floor < floors; floor++) {
                        Task t = createTask(project, milestone, templateTask, floor);
                        t = taskRepo.save(t);
                        taskIndex.put(new TaskFloorKey(templateTask.getId(), floor), t.getId());
                        tasksCreated++;
                    }
                }
            }
        }

        // Replay predecessors with cross-floor expansion.
        int predsCreated = 0;
        for (WbsTemplateTaskPredecessor edge : templateEdges) {
            WbsTemplateTask succT = edge.getSuccessor();
            WbsTemplateTask predT = edge.getPredecessor();
            FloorLoop succLoop = succT.getFloorLoop();
            FloorLoop predLoop = predT.getFloorLoop();
            int lag = edge.getLagDays() != null ? edge.getLagDays() : 0;
            String depType = edge.getDepType() != null ? edge.getDepType() : "FS";

            if (succLoop == FloorLoop.PER_FLOOR && predLoop == FloorLoop.PER_FLOOR) {
                for (int floor = 0; floor < floors; floor++) {
                    Long succId = taskIndex.get(new TaskFloorKey(succT.getId(), floor));
                    Long predId = taskIndex.get(new TaskFloorKey(predT.getId(), floor));
                    if (succId != null && predId != null) {
                        savePredecessor(succId, predId, lag, depType);
                        predsCreated++;
                    }
                }
            } else if (succLoop == FloorLoop.PER_FLOOR && predLoop == FloorLoop.NONE) {
                Long predId = taskIndex.get(new TaskFloorKey(predT.getId(), -1));
                for (int floor = 0; floor < floors; floor++) {
                    Long succId = taskIndex.get(new TaskFloorKey(succT.getId(), floor));
                    if (succId != null && predId != null) {
                        savePredecessor(succId, predId, lag, depType);
                        predsCreated++;
                    }
                }
            } else if (succLoop == FloorLoop.NONE && predLoop == FloorLoop.PER_FLOOR) {
                Long succId = taskIndex.get(new TaskFloorKey(succT.getId(), -1));
                Long predId = taskIndex.get(new TaskFloorKey(predT.getId(), floors - 1));
                if (succId != null && predId != null) {
                    savePredecessor(succId, predId, lag, depType);
                    predsCreated++;
                }
            } else {
                Long succId = taskIndex.get(new TaskFloorKey(succT.getId(), -1));
                Long predId = taskIndex.get(new TaskFloorKey(predT.getId(), -1));
                if (succId != null && predId != null) {
                    savePredecessor(succId, predId, lag, depType);
                    predsCreated++;
                }
            }
        }

        // Dual-write to legacy Task.dependsOnTaskId for tasks with exactly one
        // predecessor (mirrors TaskPredecessorService behaviour from PR1).
        applyLegacyDependsOnDualWrite(project.getId());

        log.info("Cloned WBS template {} v{} into project {}: {} milestones, {} tasks, {} predecessors",
                 template.getCode(), template.getVersion(), project.getId(),
                 milestonesCreated, tasksCreated, predsCreated);
        return new WbsCloneResult(milestonesCreated, tasksCreated, predsCreated);
    }

    private void savePredecessor(Long successorId, Long predecessorId, int lagDays, String depType) {
        TaskPredecessor row = new TaskPredecessor(successorId, predecessorId, lagDays);
        row.setDepType(depType);
        taskPredecessorRepo.save(row);
    }

    /**
     * For every task in the just-cloned project, if it has exactly one row in
     * task_predecessor, mirror that predecessor id onto Task.depends_on_task_id;
     * otherwise leave it null. Matches the contract enforced by
     * {@link TaskPredecessorService#replacePredecessors}.
     */
    private void applyLegacyDependsOnDualWrite(Long projectId) {
        taskRepo.flush();
        taskPredecessorRepo.flush();
        for (Task t : taskRepo.findByProjectId(projectId)) {
            long n = taskPredecessorRepo.countBySuccessorId(t.getId());
            if (n == 1L) {
                List<TaskPredecessor> ps = taskPredecessorRepo.findBySuccessorId(t.getId());
                if (!ps.isEmpty()) {
                    Long onlyPred = ps.get(0).getPredecessorId();
                    if (!onlyPred.equals(t.getDependsOnTaskId())) {
                        t.setDependsOnTaskId(onlyPred);
                        taskRepo.save(t);
                    }
                }
            }
        }
    }

    private int resolveFloorCount(CustomerProject project, Integer requested) {
        if (requested != null && requested > 0) return requested;
        if (project.getFloors() != null && project.getFloors() > 0) return project.getFloors();
        throw new IllegalArgumentException(
                "floorCount must be provided either in the request or on the project");
    }

    private Task createTask(CustomerProject project, ProjectMilestone milestone,
                            WbsTemplateTask src, int floorIndex) {
        Task t = new Task();
        String suffix = floorIndex >= 0 ? " — Floor " + floorIndex : "";
        t.setTitle(src.getName() + suffix);
        // Do NOT copy roleHint into description; roleHint is a role code, not
        // user-facing copy. Let the admin UI populate descriptions explicitly.
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(project);
        t.setMilestoneId(milestone.getId());
        t.setMonsoonSensitive(Boolean.TRUE.equals(src.getMonsoonSensitive()));
        // For S1, leave start/end null — CPM (S2) computes them. dueDate is
        // mandatory on the entity; fall back to project start + duration.
        LocalDate base = project.getStartDate() != null ? project.getStartDate() : LocalDate.now();
        int days = src.getDurationDays() != null ? src.getDurationDays() : 1;
        t.setDueDate(base.plusDays(days));
        return t;
    }

    private record TaskFloorKey(Long templateTaskId, int floorIndex) {}
}
