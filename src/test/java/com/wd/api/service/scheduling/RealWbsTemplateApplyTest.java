package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.Task;
import com.wd.api.model.TaskQualityGate;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectMilestoneRepository;
import com.wd.api.repository.TaskQualityGateRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.service.scheduling.dto.WbsCloneResult;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD integration test for the "apply real YAML-seeded WBS template to a
 * project" path. Differs from {@link WbsTemplateClonerServiceTest} (which
 * exercises the cloner against a synthetic in-test fixture) by driving the
 * cloner against the four production templates loaded by
 * {@link WbsTemplateSeeder} from {@code classpath:db/seed/wbs/*.yaml}.
 *
 * <p>Adds two contracts the audit asked for:
 * <ol>
 *   <li>All four project types ({@code RESIDENTIAL}, {@code COMMERCIAL},
 *       {@code INTERIOR_FITOUT}, {@code RENOVATION}) clone successfully into
 *       a fresh project of the matching type, producing non-empty phases and
 *       tasks.</li>
 *   <li>Every materialised task ends up with three {@code task_quality_gates}
 *       rows (PRELIMINARY / IN_PROGRESS / FINAL), all in PENDING status — i.e.
 *       the cloner's hook into {@link com.wd.api.service.TaskQualityGateService}
 *       fires for templated tasks the same way it does for manual ones.</li>
 * </ol>
 *
 * <p>These tests are independent of {@code project 50} (the live target); the
 * actual application to project 50 is an operational follow-up performed via
 * {@code POST /api/projects/50/wbs/clone-from-template}.
 */
@Transactional
class RealWbsTemplateApplyTest extends TestcontainersPostgresBase {

    @Autowired private WbsTemplateSeeder seeder;
    @Autowired private WbsTemplateClonerService cloner;
    @Autowired private CpmService cpmService;
    @Autowired private WbsTemplateRepository templates;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private ProjectMilestoneRepository milestones;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskQualityGateRepository qcGates;

    @Test
    void allFourTemplates_seedActiveAndCloneable() throws Exception {
        seeder.seedFromClasspath();

        for (String code : List.of("RESIDENTIAL", "COMMERCIAL", "INTERIOR_FITOUT", "RENOVATION")) {
            Optional<WbsTemplate> tpl = templates.findByCodeAndIsActiveTrue(code);
            assertThat(tpl).as("active template %s exists", code).isPresent();
            assertThat(tpl.get().getProjectType()).isEqualTo(code);

            CustomerProject project = newProject(code, /* floors */ 2);
            WbsCloneResult result = cloner.cloneInto(project, tpl.get().getId(), 2);

            assertThat(result.milestonesCreated())
                    .as("%s template should materialise >= 1 phase", code)
                    .isGreaterThanOrEqualTo(1);
            assertThat(result.tasksCreated())
                    .as("%s template should materialise >= 1 task", code)
                    .isGreaterThanOrEqualTo(1);

            // Tasks linked to project + milestone
            List<Task> projTasks = tasks.findByProjectId(project.getId());
            assertThat(projTasks).hasSize(result.tasksCreated());
            assertThat(projTasks).allSatisfy(t ->
                    assertThat(t.getMilestoneId()).isNotNull());

            // Milestones linked to project
            List<ProjectMilestone> projMilestones = milestones.findByProjectId(project.getId());
            assertThat(projMilestones).hasSize(result.milestonesCreated());
        }
    }

    @Test
    void residentialTemplate_cloneIntoFreshProject_seedsThreeQcGatesPerTask() throws Exception {
        seeder.seedFromClasspath();
        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL")
                .orElseThrow(() -> new AssertionError(
                        "RESIDENTIAL template must seed for this test to run"));

        CustomerProject project = newProject("RESIDENTIAL", /* floors */ 2);
        WbsCloneResult result = cloner.cloneInto(project, residential.getId(), 2);

        assertThat(result.tasksCreated()).isGreaterThan(0);

        List<Task> projTasks = tasks.findByProjectId(project.getId());
        assertThat(projTasks)
                .as("residential template must materialise tasks")
                .isNotEmpty();

        // Every task must have exactly 3 PENDING gates (PRELIM, IN_PROGRESS, FINAL).
        // This catches regressions where WbsTemplateClonerService forgets to
        // call qualityGateService.seedGatesFor(savedTask).
        for (Task t : projTasks) {
            List<TaskQualityGate> gates = qcGates.findByTaskId(t.getId());
            assertThat(gates)
                    .as("task %s ('%s') gates", t.getId(), t.getTitle())
                    .hasSize(3)
                    .extracting(TaskQualityGate::getGateType)
                    .containsExactlyInAnyOrder(
                            TaskQualityGate.GateType.PRELIMINARY,
                            TaskQualityGate.GateType.IN_PROGRESS,
                            TaskQualityGate.GateType.FINAL);
            assertThat(gates)
                    .extracting(TaskQualityGate::getStatus)
                    .allMatch(s -> s == TaskQualityGate.Status.PENDING);
        }
    }

    /**
     * RED test — before the fix, this fails because CpmService.durationDays(Task)
     * only derives from start_date/end_date, ignoring the Task.durationDays
     * column the cloner populated from the YAML. Result: every task computed as
     * 0-day duration, project finishes the day it starts, every task isCritical.
     */
    @Test
    void cpmRespectsTemplateDurations_residentialProjectSpansMonths() throws Exception {
        seeder.seedFromClasspath();
        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL")
                .orElseThrow();

        CustomerProject project = newProject("RESIDENTIAL", 2);
        project.setStartDate(LocalDate.of(2026, 6, 1));
        projects.save(project);

        cloner.cloneInto(project, residential.getId(), 2);
        cpmService.recompute(project.getId());

        List<Task> projTasks = tasks.findByProjectId(project.getId());
        assertThat(projTasks).isNotEmpty();

        // (1) Every cloned task must have durationDays set from template.
        assertThat(projTasks).allSatisfy(t -> {
            assertThat(t.getDurationDays())
                    .as("task #%s '%s' durationDays", t.getId(), t.getTitle())
                    .isNotNull()
                    .isGreaterThan(0);
        });

        // (2) CPM must have populated es/ef on every task.
        assertThat(projTasks).allSatisfy(t -> {
            assertThat(t.getEsDate())
                    .as("task #%s esDate", t.getId()).isNotNull();
            assertThat(t.getEfDate())
                    .as("task #%s efDate", t.getId()).isNotNull();
        });

        // (3) Project span must reflect template durations (longest path).
        // Residential serial path Excavation(4)+PCC(2)+Foundation(7)+
        // ColumnCasting×2(6)+SlabCasting×2(8)+BlockWork×2(12)+(Electrical||Plumbing)(4)+
        // Plastering×2(12)+Flooring×2(10)+Painting×2(10)+SnagList(3)+Cleaning(2)+
        // Handover(1) ≈ 80+ working days. Allow generous slack (>30) for working-day
        // calendar nuances.
        LocalDate earliestStart = projTasks.stream()
                .map(Task::getEsDate)
                .filter(d -> d != null)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate latestFinish = projTasks.stream()
                .map(Task::getEfDate)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        long spanDays = ChronoUnit.DAYS.between(earliestStart, latestFinish);
        assertThat(spanDays)
                .as("residential project span (es to ef)")
                .isGreaterThan(30);

        // (4) Not EVERY task should be critical — residential has parallel paths
        // (Electrical Rough-in || Plumbing both feed Plastering). With correct
        // durations, the shorter parallel path has positive float.
        long total = projTasks.size();
        long critical = projTasks.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsCritical()))
                .count();
        assertThat(critical)
                .as("not every task should be critical (parallel paths exist)")
                .isLessThan(total);
    }

    @Test
    void cloningTwiceIntoSameProject_isRejectedWith409Semantic() throws Exception {
        seeder.seedFromClasspath();
        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL")
                .orElseThrow();

        CustomerProject project = newProject("RESIDENTIAL", 2);
        cloner.cloneInto(project, residential.getId(), 2);

        // Second clone must fail — the cloner has a single-shot guard so a
        // double-application doesn't silently duplicate the entire WBS.
        assertThatThrownBy(() -> cloner.cloneInto(project, residential.getId(), 2))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CustomerProject newProject(String projectType, Integer floors) {
        CustomerProject p = new CustomerProject();
        p.setName("real-template-test " + UUID.randomUUID());
        p.setLocation("Test Location");
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p.setProjectUuid(UUID.randomUUID());
        p.setProjectType(projectType);
        p.setFloors(floors);
        return projects.save(p);
    }
}
