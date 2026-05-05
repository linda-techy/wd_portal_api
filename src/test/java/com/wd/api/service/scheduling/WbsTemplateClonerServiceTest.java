package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskPredecessorRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.service.scheduling.dto.WbsCloneResult;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Twelve-case verification of the cross-floor predecessor expansion in
 * {@link WbsTemplateClonerService#cloneInto}.
 *
 * <p>Builds a fixture template T_FIX in code (not from YAML) so the
 * tests are independent of seed content. Each test creates a fresh
 * CustomerProject and exercises a specific cross-floor branch.
 */
@Transactional
class WbsTemplateClonerServiceTest extends TestcontainersPostgresBase {

    @Autowired private WbsTemplateClonerService cloner;
    @Autowired private WbsTemplateRepository templates;
    @Autowired private WbsTemplatePhaseRepository phases;
    @Autowired private WbsTemplateTaskRepository templateTasks;
    @Autowired private WbsTemplateTaskPredecessorRepository templatePreds;
    @Autowired private TaskRepository tasks;
    @Autowired private TaskPredecessorRepository taskPreds;
    @Autowired private CustomerProjectRepository projects;

    @PersistenceContext private EntityManager em;

    /** Holds ids and entities of a freshly built T_FIX template. */
    private static class Fixture {
        WbsTemplate template;
        Map<String, WbsTemplateTask> taskByLabel = new HashMap<>();
    }

    private Fixture buildFixture(String code) {
        WbsTemplate t = new WbsTemplate();
        t.setCode(code);
        t.setProjectType("RESIDENTIAL");
        t.setName("T_FIX " + code);
        t = templates.save(t);

        WbsTemplatePhase p1 = new WbsTemplatePhase();
        p1.setTemplate(t); p1.setSequence(1); p1.setName("Substructure");
        p1 = phases.save(p1);

        WbsTemplatePhase p2 = new WbsTemplatePhase();
        p2.setTemplate(t); p2.setSequence(2); p2.setName("Superstructure");
        p2 = phases.save(p2);

        WbsTemplatePhase p3 = new WbsTemplatePhase();
        p3.setTemplate(t); p3.setSequence(3); p3.setName("Handover");
        p3 = phases.save(p3);

        WbsTemplateTask t1 = newTask(p1, 1, "Foundation", FloorLoop.NONE, 5, false, true);
        WbsTemplateTask t2 = newTask(p2, 1, "Slab Casting", FloorLoop.PER_FLOOR, 4, true, false);
        WbsTemplateTask t3 = newTask(p2, 2, "Block Work", FloorLoop.PER_FLOOR, 6, false, false);
        WbsTemplateTask t4 = newTask(p3, 1, "Handover", FloorLoop.NONE, 1, false, true);

        // t2 depends on t1 (NONE → PER_FLOOR fan-out)
        WbsTemplateTaskPredecessor e1 = new WbsTemplateTaskPredecessor();
        e1.setSuccessor(t2); e1.setPredecessor(t1); e1.setLagDays(0); e1.setDepType("FS");
        templatePreds.save(e1);

        // t3 depends on t2 (PER_FLOOR → PER_FLOOR same-floor)
        WbsTemplateTaskPredecessor e2 = new WbsTemplateTaskPredecessor();
        e2.setSuccessor(t3); e2.setPredecessor(t2); e2.setLagDays(0); e2.setDepType("FS");
        templatePreds.save(e2);

        // t4 depends on t3 (PER_FLOOR → NONE; top-floor source)
        WbsTemplateTaskPredecessor e3 = new WbsTemplateTaskPredecessor();
        e3.setSuccessor(t4); e3.setPredecessor(t3); e3.setLagDays(0); e3.setDepType("FS");
        templatePreds.save(e3);

        Fixture f = new Fixture();
        f.template = t;
        f.taskByLabel.put("t1", t1);
        f.taskByLabel.put("t2", t2);
        f.taskByLabel.put("t3", t3);
        f.taskByLabel.put("t4", t4);
        em.flush();
        em.clear();
        return f;
    }

    private WbsTemplateTask newTask(WbsTemplatePhase phase, int seq, String name,
                                    FloorLoop loop, int duration, boolean monsoon,
                                    boolean payment) {
        WbsTemplateTask task = new WbsTemplateTask();
        task.setPhase(phase);
        task.setSequence(seq);
        task.setName(name);
        task.setDurationDays(duration);
        task.setFloorLoop(loop);
        task.setMonsoonSensitive(monsoon);
        task.setIsPaymentMilestone(payment);
        return templateTasks.save(task);
    }

    private CustomerProject newProject(Integer floorsOnEntity) {
        CustomerProject p = new CustomerProject();
        p.setName("clone-test " + UUID.randomUUID());
        p.setLocation("Test Location");
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p.setProjectUuid(UUID.randomUUID());
        p.setFloors(floorsOnEntity);
        return projects.save(p);
    }

    private List<Task> tasksByTitlePrefix(Long projectId, String prefix) {
        return tasks.findByProjectId(projectId).stream()
                .filter(t -> t.getTitle().startsWith(prefix))
                .toList();
    }

    // ===== test cases =====

    @Test
    void gPlus2_residential_expandsPerFloorToThreeFloors() {
        Fixture f = buildFixture("FIX1");
        CustomerProject project = newProject(null);
        WbsCloneResult result = cloner.cloneInto(project, f.template.getId(), 3);

        assertThat(result.milestonesCreated()).isEqualTo(3);
        // 1 (Foundation) + 3 (Slab × floors) + 3 (Block × floors) + 1 (Handover) = 8
        assertThat(result.tasksCreated()).isEqualTo(8);

        long projectTasks = tasks.findByProjectId(project.getId()).size();
        assertThat(projectTasks).isEqualTo(8);
    }

    @Test
    void noneFloorLoopEmitsExactlyOnceRegardlessOfFloorCount() {
        Fixture f = buildFixture("FIX2");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 10);

        List<Task> foundation = tasksByTitlePrefix(project.getId(), "Foundation");
        List<Task> handover = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> t.getTitle().equals("Handover"))
                .toList();
        assertThat(foundation).hasSize(1);
        assertThat(handover).hasSize(1);
    }

    @Test
    void perFloorToPerFloorPreservesSameFloor() {
        Fixture f = buildFixture("FIX3");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 3);

        Task block1 = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Block Work — Floor 1".equals(t.getTitle()))
                .findFirst().orElseThrow();
        Task slab1 = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Slab Casting — Floor 1".equals(t.getTitle()))
                .findFirst().orElseThrow();
        List<TaskPredecessor> ps = taskPreds.findBySuccessorId(block1.getId());
        assertThat(ps).hasSize(1);
        assertThat(ps.get(0).getPredecessorId())
                .as("Block Work Floor 1 predecessor must be Slab Casting Floor 1")
                .isEqualTo(slab1.getId());
    }

    @Test
    void noneToPerFloorFansOutToAllFloors() {
        Fixture f = buildFixture("FIX4");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 3);

        Task foundation = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Foundation".equals(t.getTitle()))
                .findFirst().orElseThrow();
        for (int floor = 0; floor < 3; floor++) {
            String title = "Slab Casting — Floor " + floor;
            Task slab = tasks.findByProjectId(project.getId()).stream()
                    .filter(t -> title.equals(t.getTitle()))
                    .findFirst().orElseThrow();
            List<TaskPredecessor> ps = taskPreds.findBySuccessorId(slab.getId());
            assertThat(ps).hasSize(1);
            assertThat(ps.get(0).getPredecessorId())
                    .as("Slab Casting Floor %d must depend on Foundation", floor)
                    .isEqualTo(foundation.getId());
        }
    }

    @Test
    void perFloorToNoneUsesTopFloorAsSource() {
        Fixture f = buildFixture("FIX5");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 3);

        Task handover = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Handover".equals(t.getTitle()))
                .findFirst().orElseThrow();
        Task blockTop = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Block Work — Floor 2".equals(t.getTitle()))
                .findFirst().orElseThrow();

        List<TaskPredecessor> ps = taskPreds.findBySuccessorId(handover.getId());
        assertThat(ps).hasSize(1);
        assertThat(ps.get(0).getPredecessorId())
                .as("Handover must source from top floor (Floor 2)")
                .isEqualTo(blockTop.getId());
    }

    @Test
    void flagCarryThrough() {
        Fixture f = buildFixture("FIX6");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 3);

        // Slab Casting Floors 0/1/2 must all be monsoonSensitive=true
        for (int floor = 0; floor < 3; floor++) {
            String title = "Slab Casting — Floor " + floor;
            Task slab = tasks.findByProjectId(project.getId()).stream()
                    .filter(t -> title.equals(t.getTitle()))
                    .findFirst().orElseThrow();
            assertThat(slab.getMonsoonSensitive())
                    .as("Slab Casting Floor %d", floor).isTrue();
        }
    }

    @Test
    void durationCarryThrough() {
        Fixture f = buildFixture("FIX7");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 3);

        Task slab0 = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> "Slab Casting — Floor 0".equals(t.getTitle()))
                .findFirst().orElseThrow();
        // dueDate = startDate + 4 (templateTask.durationDays)
        assertThat(slab0.getDueDate())
                .isEqualTo(LocalDate.of(2026, 6, 1).plusDays(4));
    }

    @Test
    void floorCountOne_treatsPerFloorAsSinglePerFloor() {
        Fixture f = buildFixture("FIX8");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 1);

        List<Task> slabs = tasks.findByProjectId(project.getId()).stream()
                .filter(t -> t.getTitle().startsWith("Slab Casting"))
                .toList();
        assertThat(slabs).hasSize(1);
        assertThat(slabs.get(0).getTitle()).isEqualTo("Slab Casting — Floor 0");
    }

    @Test
    void taskNameSuffix_usesFloorIntegerNotGroundLabel() {
        Fixture f = buildFixture("FIX9");
        CustomerProject project = newProject(null);
        cloner.cloneInto(project, f.template.getId(), 2);

        boolean hasFloor0 = tasks.findByProjectId(project.getId()).stream()
                .anyMatch(t -> "Slab Casting — Floor 0".equals(t.getTitle()));
        boolean hasFloor1 = tasks.findByProjectId(project.getId()).stream()
                .anyMatch(t -> "Slab Casting — Floor 1".equals(t.getTitle()));
        boolean hasGround = tasks.findByProjectId(project.getId()).stream()
                .anyMatch(t -> t.getTitle().contains("Ground"));
        assertThat(hasFloor0).isTrue();
        assertThat(hasFloor1).isTrue();
        assertThat(hasGround).as("must NOT use Ground label").isFalse();
    }

    @Test
    void floorCountFromProject_whenRequestNotProvided() {
        Fixture f = buildFixture("FIX10");
        CustomerProject project = newProject(2);
        WbsCloneResult result = cloner.cloneInto(project, f.template.getId(), null);

        // Foundation 1 + Slab × 2 + Block × 2 + Handover 1 = 6
        assertThat(result.tasksCreated()).isEqualTo(6);
    }

    @Test
    void floorCountMissingEverywhere_throws() {
        Fixture f = buildFixture("FIX11");
        CustomerProject project = newProject(null);

        assertThatThrownBy(() -> cloner.cloneInto(project, f.template.getId(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("floorCount");
    }

    @Test
    void unknownTemplate_throwsEntityNotFound() {
        CustomerProject project = newProject(2);

        assertThatThrownBy(() -> cloner.cloneInto(project, 999_999L, 2))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void cloneTwice_throwsIllegalState() {
        Fixture f = buildFixture("FIX_CLONE_TWICE");
        CustomerProject project = newProject(null);

        // First clone succeeds.
        cloner.cloneInto(project, f.template.getId(), 2);

        // Second clone should be rejected, not silently double the WBS.
        assertThatThrownBy(() -> cloner.cloneInto(project, f.template.getId(), 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a WBS");
    }
}
