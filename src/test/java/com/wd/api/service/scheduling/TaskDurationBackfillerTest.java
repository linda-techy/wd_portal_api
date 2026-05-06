package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayRecurrenceType;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.HolidayRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies TaskDurationBackfiller populates {@code duration_days} for
 * existing Tasks where it is null AND start_date + end_date are present.
 * Tasks without dates stay null. Tasks already populated are unchanged
 * (idempotent re-runs).
 */
@Transactional
class TaskDurationBackfillerTest extends TestcontainersPostgresBase {

    @Autowired private TaskDurationBackfiller backfiller;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private ProjectScheduleConfigRepository configs;
    @Autowired private HolidayRepository holidays;

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("Backfill-" + UUID.randomUUID());
        p.setLocation("Test");
        p.setIsDesignAgreementSigned(false);
        return projects.save(p);
    }

    private void newConfig(Long projectId, boolean sundayWorking) {
        ProjectScheduleConfig cfg = new ProjectScheduleConfig();
        cfg.setProjectId(projectId);
        cfg.setSundayWorking(sundayWorking);
        configs.save(cfg);
    }

    private Task newTask(CustomerProject p, String title, LocalDate start, LocalDate end,
                         Integer existingDuration) {
        Task t = new Task();
        t.setTitle(title);
        t.setProject(p);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setDueDate(end != null ? end : LocalDate.of(2026, 12, 31));
        t.setStartDate(start);
        t.setEndDate(end);
        t.setDurationDays(existingDuration);
        return tasks.save(t);
    }

    @Test
    void backfills_durationDays_from_start_and_end_via_workingDayCalculator() {
        CustomerProject p = newProject();
        newConfig(p.getId(), false);
        // Mon 2026-06-01 → Fri 2026-06-05; no holidays; sundayWorking=false.
        // workingDaysBetween(Mon, Fri) = 4 (Mon→Tue, Tue→Wed, Wed→Thu, Thu→Fri).
        Task t = newTask(p, "Excavation", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), null);

        backfiller.runBackfill();

        Task reloaded = tasks.findById(t.getId()).orElseThrow();
        assertThat(reloaded.getDurationDays()).isEqualTo(4);
    }

    @Test
    void leaves_durationDays_null_when_dates_are_missing() {
        CustomerProject p = newProject();
        newConfig(p.getId(), false);
        Task noDates    = newTask(p, "NoDates", null, null, null);
        Task onlyStart  = newTask(p, "StartOnly", LocalDate.of(2026, 6, 1), null, null);
        Task onlyEnd    = newTask(p, "EndOnly", null, LocalDate.of(2026, 6, 5), null);

        backfiller.runBackfill();

        assertThat(tasks.findById(noDates.getId()).orElseThrow().getDurationDays()).isNull();
        assertThat(tasks.findById(onlyStart.getId()).orElseThrow().getDurationDays()).isNull();
        assertThat(tasks.findById(onlyEnd.getId()).orElseThrow().getDurationDays()).isNull();
    }

    @Test
    void idempotent_secondRun_does_not_change_already_populated_rows() {
        CustomerProject p = newProject();
        newConfig(p.getId(), false);
        // Pre-populate duration to a sentinel that the calculator would NEVER produce.
        Task t = newTask(p, "PrePopulated", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 99);

        backfiller.runBackfill();
        Task afterFirst = tasks.findById(t.getId()).orElseThrow();
        assertThat(afterFirst.getDurationDays())
                .as("backfiller skips already-populated rows")
                .isEqualTo(99);

        backfiller.runBackfill();
        Task afterSecond = tasks.findById(t.getId()).orElseThrow();
        assertThat(afterSecond.getDurationDays()).isEqualTo(99);
    }

    @Test
    void honours_holidays_when_computing_workingDays() {
        CustomerProject p = newProject();
        newConfig(p.getId(), false);
        // Seed Wed 2026-06-03 as a NATIONAL holiday.
        Holiday h = new Holiday();
        h.setScope(HolidayScope.NATIONAL);
        h.setDate(LocalDate.of(2026, 6, 3));
        h.setName("Mid-week");
        h.setRecurrenceType(HolidayRecurrenceType.ONE_OFF);
        holidays.save(h);

        // Mon 2026-06-01 → Fri 2026-06-05; sundayWorking=false.
        // Without holidays = 4. With Wed excluded = 3.
        Task t = newTask(p, "WithHoliday", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), null);

        backfiller.runBackfill();

        assertThat(tasks.findById(t.getId()).orElseThrow().getDurationDays()).isEqualTo(3);
    }
}
