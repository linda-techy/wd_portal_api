package com.wd.api.service.changerequest;

import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.Task;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskPredecessorRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.scheduling.DelayApplier;
import com.wd.api.service.scheduling.HandoverShiftDetector;
import com.wd.api.service.scheduling.HolidayService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Materialises an APPROVED Change Request's proposed scope into the project
 * WBS at a scheduler-supplied anchor task. Mirrors
 * {@link com.wd.api.service.scheduling.WbsTemplateClonerService} for the
 * shape of the clone (NONE / PER_FLOOR), then anchors the first sequence-1
 * cloned task to the chosen anchor with a 0-day predecessor edge.
 *
 * <p>After the clone, applies the CR's {@code timeImpactWorkingDays} via
 * {@link DelayApplier} (which shifts every PENDING task on the project,
 * including the freshly inserted ones), then triggers a CPM recompute and
 * the handover-shift detector for customer notification.
 *
 * <p>The whole thing is one transaction. Cycle detection inside
 * TaskPredecessorRepository's PostgreSQL constraints OR cycle detection by
 * {@link com.wd.api.service.scheduling.TaskGraphValidator} would surface as
 * an exception that rolls back the entire merge — no orphan rows.
 *
 * <p>Cost-impact propagation: the CR holds {@code costImpact}; the merge
 * service does NOT mutate any project-budget table. Variance reporting
 * (S2 PR2's VarianceService) sees the cost on the CR and surfaces it.
 *
 * <p>Baseline preservation: {@code task_baseline} is NOT updated. CR-merged
 * tasks appear as scope creep in Plan-vs-Baseline variance reports because
 * they have no baseline counterpart.
 */
@Service
public class ChangeRequestMergeService {

    private static final Logger log = LoggerFactory.getLogger(ChangeRequestMergeService.class);

    private final ChangeRequestTaskRepository crTaskRepo;
    private final ChangeRequestTaskPredecessorRepository crPredRepo;
    private final ProjectVariationRepository crRepo;
    private final TaskRepository taskRepo;
    private final TaskPredecessorRepository taskPredRepo;
    private final ProjectScheduleConfigRepository scheduleConfigRepo;
    private final HolidayService holidayService;
    private final CpmService cpmService;
    private final HandoverShiftDetector handoverShiftDetector;

    public ChangeRequestMergeService(ChangeRequestTaskRepository crTaskRepo,
                                     ChangeRequestTaskPredecessorRepository crPredRepo,
                                     ProjectVariationRepository crRepo,
                                     TaskRepository taskRepo,
                                     TaskPredecessorRepository taskPredRepo,
                                     ProjectScheduleConfigRepository scheduleConfigRepo,
                                     HolidayService holidayService,
                                     CpmService cpmService,
                                     HandoverShiftDetector handoverShiftDetector) {
        this.crTaskRepo = crTaskRepo;
        this.crPredRepo = crPredRepo;
        this.crRepo = crRepo;
        this.taskRepo = taskRepo;
        this.taskPredRepo = taskPredRepo;
        this.scheduleConfigRepo = scheduleConfigRepo;
        this.holidayService = holidayService;
        this.cpmService = cpmService;
        this.handoverShiftDetector = handoverShiftDetector;
    }

    @Transactional
    public ChangeRequestMergeResult mergeIntoWbs(Long crId, Long anchorTaskId, Long actorUserId) {
        ProjectVariation cr = crRepo.findById(crId)
                .orElseThrow(() -> new EntityNotFoundException("CR " + crId));
        if (cr.getStatus() != VariationStatus.APPROVED) {
            throw new IllegalStateException(
                    "CR " + crId + " is " + cr.getStatus() + "; merge requires APPROVED");
        }
        Task anchor = taskRepo.findById(anchorTaskId)
                .orElseThrow(() -> new EntityNotFoundException("anchor Task " + anchorTaskId));

        CustomerProject crProject = cr.getProject();
        if (crProject == null || anchor.getProject() == null
                || !crProject.getId().equals(anchor.getProject().getId())) {
            throw new IllegalArgumentException(
                    "anchor task " + anchorTaskId + " is not part of CR " + crId + "'s project");
        }
        Long projectId = crProject.getId();
        int floorCount = crProject.getFloors() != null && crProject.getFloors() > 0
                ? crProject.getFloors() : 1;

        List<ChangeRequestTask> proposed =
                crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(crId);

        // (crTaskId, floorIndex) -> newTaskId. floorIndex = -1 for NONE.
        Map<TaskFloorKey, Long> idMap = new HashMap<>();
        int tasksCreated = 0;

        for (ChangeRequestTask crt : proposed) {
            if (crt.getFloorLoop() == FloorLoop.NONE) {
                Task t = clone(crt, anchor, crProject, -1);
                t = taskRepo.save(t);
                idMap.put(new TaskFloorKey(crt.getId(), -1), t.getId());
                tasksCreated++;
            } else {
                for (int floor = 0; floor < floorCount; floor++) {
                    Task t = clone(crt, anchor, crProject, floor);
                    t = taskRepo.save(t);
                    idMap.put(new TaskFloorKey(crt.getId(), floor), t.getId());
                    tasksCreated++;
                }
            }
        }

        // Replay CR predecessor edges with the same cross-floor expansion as
        // WbsTemplateClonerService.
        int predsCreated = 0;
        List<ChangeRequestTaskPredecessor> edges = crPredRepo.findByChangeRequestId(crId);
        for (ChangeRequestTaskPredecessor edge : edges) {
            ChangeRequestTask succT = findById(proposed, edge.getSuccessorCrTaskId());
            ChangeRequestTask predT = findById(proposed, edge.getPredecessorCrTaskId());
            if (succT == null || predT == null) continue;
            FloorLoop succLoop = succT.getFloorLoop();
            FloorLoop predLoop = predT.getFloorLoop();
            int lag = edge.getLagDays() != null ? edge.getLagDays() : 0;

            if (succLoop == FloorLoop.PER_FLOOR && predLoop == FloorLoop.PER_FLOOR) {
                for (int floor = 0; floor < floorCount; floor++) {
                    Long s = idMap.get(new TaskFloorKey(succT.getId(), floor));
                    Long p = idMap.get(new TaskFloorKey(predT.getId(), floor));
                    if (s != null && p != null) {
                        taskPredRepo.save(new TaskPredecessor(s, p, lag));
                        predsCreated++;
                    }
                }
            } else if (succLoop == FloorLoop.PER_FLOOR && predLoop == FloorLoop.NONE) {
                Long p = idMap.get(new TaskFloorKey(predT.getId(), -1));
                for (int floor = 0; floor < floorCount; floor++) {
                    Long s = idMap.get(new TaskFloorKey(succT.getId(), floor));
                    if (s != null && p != null) {
                        taskPredRepo.save(new TaskPredecessor(s, p, lag));
                        predsCreated++;
                    }
                }
            } else if (succLoop == FloorLoop.NONE && predLoop == FloorLoop.PER_FLOOR) {
                Long s = idMap.get(new TaskFloorKey(succT.getId(), -1));
                Long p = idMap.get(new TaskFloorKey(predT.getId(), floorCount - 1));
                if (s != null && p != null) {
                    taskPredRepo.save(new TaskPredecessor(s, p, lag));
                    predsCreated++;
                }
            } else {
                Long s = idMap.get(new TaskFloorKey(succT.getId(), -1));
                Long p = idMap.get(new TaskFloorKey(predT.getId(), -1));
                if (s != null && p != null) {
                    taskPredRepo.save(new TaskPredecessor(s, p, lag));
                    predsCreated++;
                }
            }
        }

        // Anchor: every floor instance of the first-by-sequence proposed task
        // (or the single instance if NONE) gets the chosen anchor as predecessor.
        if (!proposed.isEmpty()) {
            ChangeRequestTask first = proposed.get(0);
            if (first.getFloorLoop() == FloorLoop.NONE) {
                Long s = idMap.get(new TaskFloorKey(first.getId(), -1));
                if (s != null) {
                    taskPredRepo.save(new TaskPredecessor(s, anchor.getId(), 0));
                    predsCreated++;
                }
            } else {
                for (int floor = 0; floor < floorCount; floor++) {
                    Long s = idMap.get(new TaskFloorKey(first.getId(), floor));
                    if (s != null) {
                        taskPredRepo.save(new TaskPredecessor(s, anchor.getId(), 0));
                        predsCreated++;
                    }
                }
            }
        }

        // Time-impact: shift every PENDING task on the project by N working
        // days (mirrors how a delay log feeds DelayApplier — see S3 PR3).
        // We synthesise a transient (un-saved) DelayLog purely to feed the
        // existing API; the CR-merge transaction is the only persistence of
        // record. The freshly inserted PENDING tasks above are also shifted,
        // which is the desired behaviour: a customer-approved CR adds N working
        // days; everything not yet started moves out by N.
        Integer timeImpact = cr.getTimeImpactWorkingDays();
        if (timeImpact != null && timeImpact > 0) {
            DelayLog synthetic = new DelayLog();
            synthetic.setProject(crProject);
            synthetic.setDurationDays(timeImpact);
            boolean sundayWorking = scheduleConfigRepo.findByProjectId(projectId)
                    .map(cfg -> Boolean.TRUE.equals(cfg.getSundayWorking()))
                    .orElse(false);
            new DelayApplier(taskRepo, holidayService, sundayWorking)
                    .applyDelayToTasks(synthetic);
        }

        // CPM recompute + customer alert if handover shifted by > 3 working days.
        cpmService.recompute(projectId);
        handoverShiftDetector.checkAndAlert(projectId);

        log.info("Merged CR {} into project {}: {} tasks, {} predecessor edges, time-impact={} wd",
                crId, projectId, tasksCreated, predsCreated,
                timeImpact == null ? 0 : timeImpact);

        return new ChangeRequestMergeResult(tasksCreated, proposed.size(), predsCreated, /*handoverCheckRan*/ true);
    }

    private Task clone(ChangeRequestTask src, Task anchor, CustomerProject project, int floorIndex) {
        Task t = new Task();
        String suffix = floorIndex >= 0 ? " — Floor " + floorIndex : "";
        t.setTitle(src.getName() + suffix);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(project);
        t.setMilestoneId(anchor.getMilestoneId());
        t.setMonsoonSensitive(Boolean.TRUE.equals(src.getMonsoonSensitive()));
        // Mirror WbsTemplateClonerService.createTask field-mapping:
        // weight + duration_days carry; is_payment_milestone is template-only
        // metadata not copied onto Task (Task has no such column — verified).
        t.setWeight(src.getWeightFactor());
        t.setDurationDays(src.getDurationDays());
        // Start/end dates left null; CPM will compute. dueDate is mandatory, so
        // synthesise one from the anchor's dueDate as a safe default; CPM may
        // overwrite later.
        LocalDate base = anchor.getDueDate() != null ? anchor.getDueDate() : LocalDate.now();
        int days = src.getDurationDays() != null ? src.getDurationDays() : 1;
        t.setDueDate(base.plusDays(days));
        return t;
    }

    private static ChangeRequestTask findById(List<ChangeRequestTask> list, Long id) {
        for (ChangeRequestTask t : list) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    private record TaskFloorKey(Long crTaskId, int floorIndex) {}
}
