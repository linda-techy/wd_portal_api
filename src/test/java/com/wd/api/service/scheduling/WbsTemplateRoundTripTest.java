package com.wd.api.service.scheduling;

import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip persistence test for the four wbs_template_* entities.
 *
 * <p>Persists a WbsTemplate with phases, tasks (including PER_FLOOR and
 * NONE flavours), and a predecessor edge. Flushes and clears the
 * persistence context, then reloads and asserts that every field — including
 * timestamps populated by the @PrePersist hook and the explicit defaults
 * (isActive, version, monsoonSensitive, depType, lagDays) — round-trips
 * correctly through the Hibernate-built schema.
 */
@Transactional
class WbsTemplateRoundTripTest extends TestcontainersPostgresBase {

    @PersistenceContext
    private EntityManager em;

    @Test
    void wbsTemplate_roundTripsCoreFields() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("RESIDENTIAL");
        t.setProjectType("RESIDENTIAL");
        t.setName("Residential Default");
        t.setDescription("desc");
        t.setSourceHash("abc123");

        em.persist(t);
        em.flush();
        em.clear();

        WbsTemplate fetched = em.find(WbsTemplate.class, t.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getCode()).isEqualTo("RESIDENTIAL");
        assertThat(fetched.getProjectType()).isEqualTo("RESIDENTIAL");
        assertThat(fetched.getName()).isEqualTo("Residential Default");
        assertThat(fetched.getDescription()).isEqualTo("desc");
        assertThat(fetched.getSourceHash()).isEqualTo("abc123");
        assertThat(fetched.getVersion()).isEqualTo(1);
        assertThat(fetched.getIsActive()).isTrue();
        assertThat(fetched.getCreatedAt()).isNotNull();
        assertThat(fetched.getUpdatedAt()).isNotNull();
    }

    @Test
    void wbsTemplatePhase_roundTrips() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("RT_PHASE");
        t.setProjectType("RESIDENTIAL");
        t.setName("RT phase template");
        em.persist(t);

        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t);
        p.setSequence(1);
        p.setName("Substructure");
        p.setRoleHint("SITE_ENGINEER");
        p.setMonsoonSensitive(true);
        em.persist(p);
        em.flush();
        em.clear();

        WbsTemplatePhase fetched = em.find(WbsTemplatePhase.class, p.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getSequence()).isEqualTo(1);
        assertThat(fetched.getName()).isEqualTo("Substructure");
        assertThat(fetched.getRoleHint()).isEqualTo("SITE_ENGINEER");
        assertThat(fetched.getMonsoonSensitive()).isTrue();
        assertThat(fetched.getTemplate().getCode()).isEqualTo("RT_PHASE");
    }

    @Test
    void wbsTemplateTask_roundTripsPerFloorAndFlags() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("RT_TASK");
        t.setProjectType("RESIDENTIAL");
        t.setName("RT task template");
        em.persist(t);

        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t);
        p.setSequence(1);
        p.setName("Superstructure");
        em.persist(p);

        WbsTemplateTask task = new WbsTemplateTask();
        task.setPhase(p);
        task.setSequence(1);
        task.setName("Slab Casting");
        task.setRoleHint("SITE_ENGINEER");
        task.setDurationDays(5);
        task.setWeightFactor(null);
        task.setMonsoonSensitive(true);
        task.setIsPaymentMilestone(false);
        task.setFloorLoop(FloorLoop.PER_FLOOR);
        em.persist(task);
        em.flush();
        em.clear();

        WbsTemplateTask fetched = em.find(WbsTemplateTask.class, task.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getName()).isEqualTo("Slab Casting");
        assertThat(fetched.getDurationDays()).isEqualTo(5);
        assertThat(fetched.getWeightFactor()).isNull();
        assertThat(fetched.getMonsoonSensitive()).isTrue();
        assertThat(fetched.getIsPaymentMilestone()).isFalse();
        assertThat(fetched.getFloorLoop()).isEqualTo(FloorLoop.PER_FLOOR);
    }

    @Test
    void wbsTemplateTaskPredecessor_roundTripsLagAndDepType() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("RT_PRED");
        t.setProjectType("RESIDENTIAL");
        t.setName("RT pred template");
        em.persist(t);

        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t);
        p.setSequence(1);
        p.setName("phase");
        em.persist(p);

        WbsTemplateTask predTask = new WbsTemplateTask();
        predTask.setPhase(p);
        predTask.setSequence(1);
        predTask.setName("pred");
        predTask.setDurationDays(3);
        em.persist(predTask);

        WbsTemplateTask succTask = new WbsTemplateTask();
        succTask.setPhase(p);
        succTask.setSequence(2);
        succTask.setName("succ");
        succTask.setDurationDays(4);
        em.persist(succTask);

        WbsTemplateTaskPredecessor edge = new WbsTemplateTaskPredecessor();
        edge.setSuccessor(succTask);
        edge.setPredecessor(predTask);
        edge.setLagDays(2);
        edge.setDepType("FS");
        em.persist(edge);
        em.flush();
        em.clear();

        WbsTemplateTaskPredecessor fetched = em.find(WbsTemplateTaskPredecessor.class, edge.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getLagDays()).isEqualTo(2);
        assertThat(fetched.getDepType()).isEqualTo("FS");
        assertThat(fetched.getSuccessor().getName()).isEqualTo("succ");
        assertThat(fetched.getPredecessor().getName()).isEqualTo("pred");
        assertThat(fetched.getCreatedAt()).isNotNull();
    }

    @Test
    void wbsTemplateTask_defaultsApplyWhenUnspecified() {
        WbsTemplate t = new WbsTemplate();
        t.setCode("RT_DEFAULTS");
        t.setProjectType("RESIDENTIAL");
        t.setName("Defaults Template");
        em.persist(t);

        WbsTemplatePhase p = new WbsTemplatePhase();
        p.setTemplate(t);
        p.setSequence(1);
        p.setName("phase");
        em.persist(p);

        WbsTemplateTask task = new WbsTemplateTask();
        task.setPhase(p);
        task.setSequence(1);
        task.setName("Foundation");
        task.setDurationDays(7);
        em.persist(task);
        em.flush();
        em.clear();

        WbsTemplateTask fetched = em.find(WbsTemplateTask.class, task.getId());
        assertThat(fetched.getFloorLoop()).isEqualTo(FloorLoop.NONE);
        assertThat(fetched.getMonsoonSensitive()).isFalse();
        assertThat(fetched.getIsPaymentMilestone()).isFalse();
    }
}
