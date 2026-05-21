package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.TaskQualityGate;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskQualityGateRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the project-progress rollup chain.
 *
 * <p>Reproduces — and locks in the fix for — two pre-existing shipping bugs:
 *
 * <ol>
 *   <li><b>Wiring gap.</b> Until this commit, none of the task lifecycle paths
 *       (createTask / updateTask / deleteTask / TaskProgressUpdateService /
 *       TaskCompletionService) called {@code projectProgressService
 *       .updateProjectProgress}. So {@code customer_projects.overall_progress}
 *       was frozen at whatever value the manual admin recalc endpoint had last
 *       written.</li>
 *   <li><b>Missing activity_types seed.</b> The status-derive transition in
 *       {@code TaskProgressUpdateService.updateProgress} logs a
 *       {@code TASK_STATUS_CHANGED} activity. The row never existed in
 *       {@code activity_types} → the call threw RuntimeException → the whole
 *       {@code @Transactional} method rolled back, including the just-saved
 *       progressPercent + status. V155 seeds the missing row.</li>
 * </ol>
 *
 * <p>Test strategy: clone the real RESIDENTIAL WBS template into a fresh
 * project, then drive a slider edit through {@link TaskProgressUpdateService}
 * — that's the same path the Flutter Gantt FAB hits. Assert that
 * {@code CustomerProject.overallProgress} reflects the new weighted ratio.
 */
@Transactional
class ProgressRollupOnTaskChangeIntegrationTest extends TestcontainersPostgresBase {

    @Autowired private WbsTemplateSeeder seeder;
    @Autowired private WbsTemplateClonerService cloner;
    @Autowired private com.wd.api.service.TaskProgressUpdateService progressUpdate;
    @Autowired private com.wd.api.service.TaskQualityGateService qcGates;
    @Autowired private WbsTemplateRepository templates;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskQualityGateRepository gateRepo;
    @Autowired private com.wd.api.repository.ActivityTypeRepository activityTypeRepo;

    @Test
    void completingOneTask_propagatesToProjectOverallProgress() throws Exception {
        seedRequiredActivityTypes();
        seeder.seedFromClasspath();
        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL")
                .orElseThrow();

        CustomerProject project = newProject();
        project.setStartDate(LocalDate.of(2026, 6, 1));
        projects.save(project);

        cloner.cloneInto(project, residential.getId(), 2);

        // Sanity: brand-new project has no completed tasks → progress = 0.
        CustomerProject before = projects.findById(project.getId()).orElseThrow();
        assertThat(before.getOverallProgress())
                .as("freshly-cloned project should show 0% (or null) until a task is touched")
                .isIn(null, BigDecimal.ZERO, new BigDecimal("0.00"));

        // Pick a leaf-source task (no predecessors blocking) — Excavation has
        // durationDays=4 in residential.yaml, so its weight is 4.
        List<Task> projTasks = tasks.findByProjectId(project.getId());
        Task excavation = projTasks.stream()
                .filter(t -> t.getTitle().equals("Excavation"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Excavation task expected"));

        // Pre-pass the QC gates for this task so the FINAL-gate completion
        // guard (assertCompletable) lets the progress=100 transition through.
        // We don't have a portal user available in this test harness; pass null
        // (the service tolerates it on the gate fields).
        passAllGatesFor(excavation.getId());

        // Drive a slider edit through the same service the Gantt FAB calls.
        progressUpdate.updateProgress(excavation.getId(), 100, "TDD test", /* updatedBy */ null);

        // The wiring under test: customer_projects.overall_progress should now
        // reflect Excavation's weight contribution. Total residential weight
        // sums to 95 working-days (full template), so 4/95 ≈ 4.21%.
        CustomerProject after = projects.findById(project.getId()).orElseThrow();
        assertThat(after.getOverallProgress())
                .as("project overall_progress should shift after a task completes")
                .isNotNull()
                .isGreaterThan(BigDecimal.ZERO);

        // Sanity-bound the value — must be < 100% and on the order of 4–5%.
        BigDecimal pct = after.getOverallProgress();
        assertThat(pct).isLessThan(new BigDecimal("10.00"));
        assertThat(pct).isGreaterThan(new BigDecimal("2.00"));

        // Task itself should be COMPLETED (status auto-derived from progress=100).
        Task reloaded = tasks.findById(excavation.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(reloaded.getProgressPercent()).isEqualTo(100);

        // And lastProgressUpdate should be stamped — proves the
        // updateProjectProgress path executed, not just calculateProjectProgress.
        assertThat(after.getLastProgressUpdate())
                .as("lastProgressUpdate should be stamped — proves denorm write happened")
                .isNotNull();
    }

    @Test
    void revertingProgress_pullsProjectProgressBackDown() throws Exception {
        seedRequiredActivityTypes();
        seeder.seedFromClasspath();
        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL")
                .orElseThrow();

        CustomerProject project = newProject();
        project.setStartDate(LocalDate.of(2026, 6, 1));
        projects.save(project);
        cloner.cloneInto(project, residential.getId(), 2);

        List<Task> projTasks = tasks.findByProjectId(project.getId());
        Task excavation = projTasks.stream()
                .filter(t -> t.getTitle().equals("Excavation"))
                .findFirst().orElseThrow();
        passAllGatesFor(excavation.getId());

        // Complete then revert.
        progressUpdate.updateProgress(excavation.getId(), 100, "test up", null);
        BigDecimal afterComplete = projects.findById(project.getId()).orElseThrow()
                .getOverallProgress();
        assertThat(afterComplete).isGreaterThan(BigDecimal.ZERO);

        progressUpdate.updateProgress(excavation.getId(), 0, "test down", null);
        BigDecimal afterRevert = projects.findById(project.getId()).orElseThrow()
                .getOverallProgress();
        assertThat(afterRevert)
                .as("reverting a task should drop project progress back to 0")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * The test DB uses Hibernate {@code create-drop} (no Flyway), so the seed
     * migrations (V33 BOQ types, V155 TASK_STATUS_CHANGED) don't run. Insert
     * the rows the path under test needs.
     */
    private void seedRequiredActivityTypes() {
        for (String name : List.of("TASK_STATUS_CHANGED")) {
            if (activityTypeRepo.findByName(name).isEmpty()) {
                com.wd.api.model.ActivityType at = new com.wd.api.model.ActivityType();
                at.setName(name);
                at.setDescription("test seed");
                activityTypeRepo.save(at);
            }
        }
    }

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("progress-rollup-test " + UUID.randomUUID());
        p.setLocation("Test Location");
        p.setProjectType("RESIDENTIAL");
        p.setFloors(2);
        p.setProjectUuid(UUID.randomUUID());
        return projects.save(p);
    }

    /** Mark all 3 ITP gates PASSED on a task so completion isn't blocked. */
    private void passAllGatesFor(Long taskId) {
        for (TaskQualityGate g : gateRepo.findByTaskId(taskId)) {
            g.setStatus(TaskQualityGate.Status.PASSED);
            gateRepo.save(g);
        }
    }
}
