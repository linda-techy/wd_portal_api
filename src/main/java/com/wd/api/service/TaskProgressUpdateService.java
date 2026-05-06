package com.wd.api.service;

import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import com.wd.api.repository.ProjectMilestoneRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.wbs.ProgressRollupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TaskProgressUpdateService {

    private final TaskRepository taskRepo;
    private final ProjectMilestoneRepository milestoneRepo;
    private final ProgressRollupService rollup;
    private final ActivityFeedService activityFeed;
    private final CpmService cpmService;

    public TaskProgressUpdateService(TaskRepository taskRepo,
                                      ProjectMilestoneRepository milestoneRepo,
                                      ProgressRollupService rollup,
                                      ActivityFeedService activityFeed,
                                      CpmService cpmService) {
        this.taskRepo = taskRepo;
        this.milestoneRepo = milestoneRepo;
        this.rollup = rollup;
        this.activityFeed = activityFeed;
        this.cpmService = cpmService;
    }

    @Transactional
    public Task updateProgress(Long taskId, int newProgress, String note, PortalUser updatedBy) {
        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Task.TaskStatus oldStatus = task.getStatus();
        Task.TaskStatus newStatus = deriveStatus(newProgress);

        task.setProgressPercent(newProgress);
        task.setStatus(newStatus);

        if (newStatus == Task.TaskStatus.COMPLETED && task.getActualEndDate() == null) {
            task.setActualEndDate(LocalDate.now());
        }

        Task saved = taskRepo.save(task);

        // CPM recompute: any path that mutates actualStart/EndDate (and progress
        // updates can auto-stamp actualEndDate on COMPLETED transitions) must
        // refresh ES/EF/LS/LF/totalFloat/isCritical so the Gantt critical-path
        // UI in PR3 stays in sync. Mirrors the unconditional pattern used by
        // TaskPredecessorService and GanttService.
        if (saved.getProject() != null && saved.getProject().getId() != null) {
            cpmService.recompute(saved.getProject().getId());
        }

        // Recompute parent milestone progress when source = COMPUTED
        if (saved.getMilestoneId() != null) {
            milestoneRepo.findById(saved.getMilestoneId()).ifPresent(milestone -> {
                if ("COMPUTED".equals(milestone.getProgressSource())) {
                    java.util.List<Task> siblings = taskRepo.findByMilestoneId(milestone.getId());
                    java.util.List<ProgressRollupService.TaskInput> inputs = siblings.stream()
                            .map(s -> new ProgressRollupService.TaskInput(
                                    s.getProgressPercent() != null ? s.getProgressPercent() : 0,
                                    1))
                            .toList();
                    java.math.BigDecimal newPct = rollup.rollupMilestone(inputs);
                    milestone.setCompletionPercentage(newPct);
                    milestoneRepo.save(milestone);
                }
            });
        }

        if (oldStatus != newStatus && task.getProject() != null) {
            String title = "Task '" + task.getTitle() + "' moved to " + newStatus.name();
            String descr = note != null && !note.isBlank()
                    ? note
                    : "Status auto-derived from progress = " + newProgress + "%";
            activityFeed.logProjectActivity("TASK_STATUS_CHANGED", title, descr, task.getProject(), updatedBy);
        }

        return saved;
    }

    private Task.TaskStatus deriveStatus(int progress) {
        if (progress == 0) return Task.TaskStatus.PENDING;
        if (progress == 100) return Task.TaskStatus.COMPLETED;
        return Task.TaskStatus.IN_PROGRESS;
    }
}
