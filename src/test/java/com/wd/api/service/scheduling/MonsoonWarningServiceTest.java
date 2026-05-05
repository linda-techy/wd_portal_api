package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.MonsoonWarning;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MonsoonWarningServiceTest extends TestcontainersPostgresBase {

    @Autowired private MonsoonWarningService warnings;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private TaskRepository tasks;
    @Autowired private ProjectScheduleConfigRepository configs;

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("monsoon-test " + UUID.randomUUID());
        p.setLocation("Test Location");
        p.setProjectUuid(UUID.randomUUID());
        return projects.save(p);
    }

    private Task addTask(CustomerProject project, String title,
                         LocalDate start, LocalDate end, boolean monsoonSensitive) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(project);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setDueDate(end != null ? end : LocalDate.now().plusYears(1));
        t.setMonsoonSensitive(monsoonSensitive);
        return tasks.save(t);
    }

    private void setMonsoonWindow(Long projectId, short startMmdd, short endMmdd) {
        ProjectScheduleConfig cfg = new ProjectScheduleConfig();
        cfg.setProjectId(projectId);
        cfg.setMonsoonStartMonthDay(startMmdd);
        cfg.setMonsoonEndMonthDay(endMmdd);
        configs.save(cfg);
    }

    @Test
    void taskFullyInsideMonsoonWindow_warnsOverlapFull() {
        CustomerProject p = newProject();
        addTask(p, "Slab", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
        assertThat(w.get(0).severity()).isEqualTo("OVERLAP_FULL");
        assertThat(w.get(0).taskName()).isEqualTo("Slab");
    }

    @Test
    void taskFullyOutsideWindow_noWarning() {
        CustomerProject p = newProject();
        addTask(p, "Civil", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30), true);

        assertThat(warnings.warningsFor(p.getId())).isEmpty();
    }

    @Test
    void taskOverlappingStart_warnsPartial() {
        CustomerProject p = newProject();
        addTask(p, "Slab Edge", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 5), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
        assertThat(w.get(0).severity()).isEqualTo("OVERLAP_PARTIAL");
    }

    @Test
    void taskOverlappingEnd_warnsPartial() {
        CustomerProject p = newProject();
        addTask(p, "Slab End", LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 10), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
        assertThat(w.get(0).severity()).isEqualTo("OVERLAP_PARTIAL");
    }

    @Test
    void nonFlaggedTask_noWarning() {
        CustomerProject p = newProject();
        addTask(p, "Inside Work", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30), false);

        assertThat(warnings.warningsFor(p.getId())).isEmpty();
    }

    @Test
    void projectOverridesMonsoonWindow() {
        CustomerProject p = newProject();
        // override to Aug 1 → Sep 15 (mmdd 801 / 915)
        setMonsoonWindow(p.getId(), (short) 801, (short) 915);

        // June overlap is OUT under override.
        addTask(p, "JuneTask", LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 20), true);
        // August overlap IS in.
        addTask(p, "AugTask", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 20), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
        assertThat(w.get(0).taskName()).isEqualTo("AugTask");
    }

    @Test
    void multiYearTask_singleWarning() {
        CustomerProject p = newProject();
        addTask(p, "LongRun", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
    }

    @Test
    void taskAcrossYearBoundary_appliesWindowToNextYear() {
        CustomerProject p = newProject();
        addTask(p, "CrossYear", LocalDate.of(2026, 12, 15), LocalDate.of(2027, 7, 15), true);

        List<MonsoonWarning> w = warnings.warningsFor(p.getId());
        assertThat(w).hasSize(1);
    }

    @Test
    void noScheduleConfig_usesDefaultWindow() {
        CustomerProject p = newProject();
        // No setMonsoonWindow call; default applies (Jun 1 → Sep 30).
        addTask(p, "AugWork", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15), true);

        assertThat(warnings.warningsFor(p.getId())).hasSize(1);
    }

    @Test
    void taskWithoutDates_skipped() {
        CustomerProject p = newProject();
        Task t = new Task();
        t.setTitle("NoStart");
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setStartDate(null);
        t.setEndDate(null);
        t.setDueDate(LocalDate.of(2026, 12, 31));
        t.setMonsoonSensitive(true);
        tasks.save(t);

        assertThat(warnings.warningsFor(p.getId())).isEmpty();
    }
}
