package com.wd.api.service.scheduling;

import com.wd.api.model.DelayLog;
import com.wd.api.model.Task;
import com.wd.api.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shifts every PENDING task on a project forward by {@code delay.durationDays}
 * working days, preserving each task's original working-day duration. Tasks
 * with status other than PENDING (IN_PROGRESS, COMPLETED, etc.) and tasks
 * missing start/end dates are left alone.
 *
 * <p>Originally introduced by S3 PR3 as a package-private nested class on
 * {@code DelayLogService}; lifted to a top-level public class by S4 PR2 so
 * {@code ChangeRequestMergeService} can reuse the algorithm with a synthetic
 * (transient) DelayLog carrying the CR's time-impact.
 */
public class DelayApplier {

    private static final Logger log = LoggerFactory.getLogger(DelayApplier.class);

    private final TaskRepository taskRepository;
    private final HolidayService holidayService;
    private final boolean sundayWorking;

    public DelayApplier(TaskRepository taskRepository,
                        HolidayService holidayService,
                        boolean sundayWorking) {
        this.taskRepository = taskRepository;
        this.holidayService = holidayService;
        this.sundayWorking = sundayWorking;
    }

    public void applyDelayToTasks(DelayLog delay) {
        if (delay == null || delay.getDurationDays() == null || delay.getDurationDays() <= 0) return;
        if (delay.getProject() == null || delay.getProject().getId() == null) return;
        Long projectId = delay.getProject().getId();
        int days = delay.getDurationDays();

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        LocalDate winStart = tasks.stream()
                .map(Task::getStartDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());
        LocalDate winEnd = tasks.stream()
                .map(Task::getEndDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(winStart)
                .plusDays(Math.max(days, 30) * 2L);
        Set<LocalDate> holidays = holidayService.holidaysFor(projectId, winStart, winEnd);

        int shifted = 0;
        for (Task t : tasks) {
            if (t.getStatus() != Task.TaskStatus.PENDING) continue;
            if (t.getStartDate() == null || t.getEndDate() == null) continue;

            int origDuration = WorkingDayCalculator.workingDaysBetween(
                    t.getStartDate(), t.getEndDate(), holidays, sundayWorking);
            LocalDate newStart = WorkingDayCalculator.addWorkingDays(
                    t.getStartDate(), days, holidays, sundayWorking);
            LocalDate newEnd = WorkingDayCalculator.addWorkingDays(
                    newStart, origDuration, holidays, sundayWorking);
            t.setStartDate(newStart);
            t.setEndDate(newEnd);
            taskRepository.save(t);
            shifted++;
        }
        log.debug("DelayApplier: project={} shifted {} pending tasks by {} working days",
                projectId, shifted, days);
    }
}
