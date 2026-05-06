package com.wd.api.service.scheduling;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies WbsTemplateClonerService copies weight_factor and duration_days
 * from each WbsTemplateTask to the materialized Task — for both NONE phase
 * tasks and PER_FLOOR floor-copies. Both fields preserved as null when the
 * template has null. Both preserved as int when the template has int.
 */
@Transactional
class WbsTemplateClonerWeightCarryThroughTest extends TestcontainersPostgresBase {

    @Autowired private WbsTemplateClonerService cloner;
    @Autowired private WbsTemplateRepository templates;
    @Autowired private WbsTemplatePhaseRepository phases;
    @Autowired private WbsTemplateTaskRepository templateTasks;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;

    private CustomerProject newProject(int floors) {
        CustomerProject p = new CustomerProject();
        p.setName("CarryThrough-" + UUID.randomUUID());
        p.setLocation("Test");
        p.setIsDesignAgreementSigned(false);
        p.setStartDate(LocalDate.of(2026, 6, 1));
        p.setFloors(floors);
        return projects.save(p);
    }

    private WbsTemplate newTemplate(String code) {
        WbsTemplate t = new WbsTemplate();
        t.setCode(code);
        t.setProjectType("RESIDENTIAL");
        t.setName("Weight Carry " + code);
        return templates.save(t);
    }

    private WbsTemplatePhase newPhase(WbsTemplate t, int seq, String name) {
        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t); p.setSequence(seq); p.setName(name);
        return phases.save(p);
    }

    private WbsTemplateTask newTask(WbsTemplatePhase phase, int seq, String name,
                                    FloorLoop loop, Integer durationDays, Integer weightFactor) {
        WbsTemplateTask tt = new WbsTemplateTask();
        tt.setPhase(phase);
        tt.setSequence(seq);
        tt.setName(name);
        tt.setFloorLoop(loop);
        tt.setDurationDays(durationDays);
        tt.setWeightFactor(weightFactor);
        return templateTasks.save(tt);
    }

    @Test
    void clone_copies_weightFactor_and_durationDays_for_NONE_phase_tasks() {
        // Note: WbsTemplateTask.duration_days is NOT NULL with CHECK >= 1
        // (V116 schema), so we always seed a positive duration. weight_factor
        // is nullable on the template — that path is exercised here.
        WbsTemplate t = newTemplate("CT-NONE-" + UUID.randomUUID());
        WbsTemplatePhase phase = newPhase(t, 1, "Foundation");
        newTask(phase, 1, "Excavation",        FloorLoop.NONE, 5, 10);
        newTask(phase, 2, "Pile cap",          FloorLoop.NONE, 3, null);
        newTask(phase, 3, "Backfill",          FloorLoop.NONE, 1, null);

        CustomerProject p = newProject(1);
        cloner.cloneInto(p, t.getId(), 1);

        List<Task> created = tasks.findByProjectId(p.getId());
        assertThat(created).hasSize(3);
        Task excavation = created.stream().filter(x -> x.getTitle().startsWith("Excavation")).findFirst().orElseThrow();
        Task pileCap    = created.stream().filter(x -> x.getTitle().startsWith("Pile cap")).findFirst().orElseThrow();
        Task backfill   = created.stream().filter(x -> x.getTitle().startsWith("Backfill")).findFirst().orElseThrow();

        assertThat(excavation.getDurationDays()).isEqualTo(5);
        assertThat(excavation.getWeight()).isEqualTo(10);

        assertThat(pileCap.getDurationDays()).isEqualTo(3);
        assertThat(pileCap.getWeight()).isNull();

        assertThat(backfill.getDurationDays()).isEqualTo(1);
        assertThat(backfill.getWeight()).isNull();
    }

    @Test
    void clone_copies_weightFactor_and_durationDays_to_every_floor_copy_for_PER_FLOOR() {
        // Same NOT NULL constraint applies: durations are always positive on
        // the template; weight_factor null is the carry-through case for null.
        WbsTemplate t = newTemplate("CT-PER-" + UUID.randomUUID());
        WbsTemplatePhase phase = newPhase(t, 1, "Superstructure");
        newTask(phase, 1, "Slab", FloorLoop.PER_FLOOR, 7, 20);
        newTask(phase, 2, "Walls", FloorLoop.PER_FLOOR, 4, null);

        CustomerProject p = newProject(3); // G + 2 = 3 floor copies per task
        cloner.cloneInto(p, t.getId(), 3);

        List<Task> created = tasks.findByProjectId(p.getId());
        // 2 templates × 3 floors = 6 tasks
        assertThat(created).hasSize(6);

        List<Task> slabs = created.stream().filter(x -> x.getTitle().startsWith("Slab")).toList();
        assertThat(slabs).hasSize(3);
        assertThat(slabs).allSatisfy(s -> {
            assertThat(s.getDurationDays()).isEqualTo(7);
            assertThat(s.getWeight()).isEqualTo(20);
        });

        List<Task> walls = created.stream().filter(x -> x.getTitle().startsWith("Walls")).toList();
        assertThat(walls).hasSize(3);
        assertThat(walls).allSatisfy(w -> {
            assertThat(w.getDurationDays()).isEqualTo(4);
            assertThat(w.getWeight()).isNull();
        });
    }
}
