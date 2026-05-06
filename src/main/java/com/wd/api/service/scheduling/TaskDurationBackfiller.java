package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Boot-time backfill of {@code tasks.duration_days} for live tasks that
 * have start_date + end_date populated but no recorded duration.
 *
 * <p>Idempotent: rows with non-null duration_days are skipped. Rows with
 * either date missing are also skipped (their duration is genuinely
 * unknown until CPM schedules them).
 *
 * <p>Disabled in tests via {@code @Profile("!test")}; tests drive the
 * backfill via the public {@link #runBackfill()} entry point.
 */
@Component
@Profile("!test")
@Order(60) // after Flyway, HolidaySeeder (40), CpmInitialPopulator (50)
public class TaskDurationBackfiller implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskDurationBackfiller.class);

    private final TaskRepository taskRepo;
    private final ProjectScheduleConfigRepository configRepo;
    private final HolidayService holidayService;

    public TaskDurationBackfiller(TaskRepository taskRepo,
                                  ProjectScheduleConfigRepository configRepo,
                                  HolidayService holidayService) {
        this.taskRepo = taskRepo;
        this.configRepo = configRepo;
        this.holidayService = holidayService;
    }

    @Override
    public void run(String... args) {
        runBackfill();
    }

    /**
     * Public entry point so tests can drive the backfill without a
     * CommandLineRunner harness. Same pattern as WbsTemplateSeeder.
     */
    @Transactional
    public void runBackfill() {
        long t0 = System.currentTimeMillis();
        int touched = 0;
        int skippedNoDates = 0;
        int skippedAlreadySet = 0;

        for (Task t : taskRepo.findAll()) {
            if (t.getDurationDays() != null) {
                skippedAlreadySet++;
                continue;
            }
            LocalDate start = t.getStartDate();
            LocalDate end = t.getEndDate();
            if (start == null || end == null) {
                skippedNoDates++;
                continue;
            }
            Long projectId = t.getProject() != null ? t.getProject().getId() : null;
            if (projectId == null) {
                skippedNoDates++;
                continue;
            }

            boolean sundayWorking = configRepo.findByProjectId(projectId)
                    .map(ProjectScheduleConfig::getSundayWorking)
                    .map(Boolean::booleanValue)
                    .orElse(false);

            Set<LocalDate> holidays = holidayService.holidaysFor(projectId, start, end);
            int days = WorkingDayCalculator.workingDaysBetween(start, end, holidays, sundayWorking);
            t.setDurationDays(days);
            taskRepo.save(t);
            touched++;
        }

        log.info("TaskDurationBackfiller: touched={} skippedAlreadySet={} skippedNoDates={} totalDuration={}ms",
                touched, skippedAlreadySet, skippedNoDates, System.currentTimeMillis() - t0);
    }
}
