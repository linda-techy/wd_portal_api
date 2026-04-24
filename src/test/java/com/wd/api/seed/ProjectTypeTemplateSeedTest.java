package com.wd.api.seed;

import com.wd.api.model.MilestoneTemplateTask;
import com.wd.api.model.ProjectTypeTemplate;
import com.wd.api.repository.MilestoneTemplateTaskRepository;
import com.wd.api.repository.ProjectTypeTemplateRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the *shape* of the V63 template schema (tables, columns, FKs, unique
 * constraints) rather than the presence of specific seed rows, because the
 * portal test base uses Hibernate ddl-auto=create and does not run the V63
 * seed INSERTs against the Testcontainers DB. Prod-side seed integrity is
 * verified once at deploy-time via the V63 migration itself.
 */
class ProjectTypeTemplateSeedTest extends TestcontainersPostgresBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProjectTypeTemplateRepository templateRepo;
    @Autowired private MilestoneTemplateTaskRepository milestoneTaskRepo;

    @Test
    void canInsertTemplateWithCodeAndRoundtrip() {
        jdbc.update(
                "INSERT INTO project_type_templates (project_type, code, description, created_at) " +
                "VALUES ('Single Floor House', 'TEST_SINGLE_FLOOR', 'test seed', NOW())");
        assertThat(templateRepo.findByCode("TEST_SINGLE_FLOOR")).isPresent();
        ProjectTypeTemplate t = templateRepo.findByCode("TEST_SINGLE_FLOOR").orElseThrow();
        assertThat(t.getCode()).isEqualTo("TEST_SINGLE_FLOOR");
    }

    @Test
    void milestoneTemplateTasksTableAcceptsForeignKey() {
        Long templateId = jdbc.queryForObject(
                "INSERT INTO project_type_templates (project_type, code, description, created_at) " +
                "VALUES ('Test FK Project', 'TEST_FK_CHECK', 'desc', NOW()) RETURNING id",
                Long.class);
        Long milestoneId = jdbc.queryForObject(
                "INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, phase, created_at) " +
                "VALUES (?, 'Walls', 1, 10.0, 'EXECUTION', NOW()) RETURNING id",
                Long.class, templateId);
        jdbc.update(
                "INSERT INTO milestone_template_tasks (milestone_template_id, task_name, task_order, estimated_days, created_at) " +
                "VALUES (?, 'Brick masonry up to roof', 1, 10, NOW())",
                milestoneId);

        List<MilestoneTemplateTask> tasks = milestoneTaskRepo.findByMilestoneTemplateIdOrderByTaskOrderAsc(milestoneId);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTaskName()).isEqualTo("Brick masonry up to roof");
    }

    @Test
    void codeColumnEnforcesUniqueness() {
        jdbc.update("INSERT INTO project_type_templates (project_type, code, description, created_at) " +
                    "VALUES ('Type A', 'TEST_UNIQ_1', 'a', NOW())");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                jdbc.update("INSERT INTO project_type_templates (project_type, code, description, created_at) " +
                            "VALUES ('Type B', 'TEST_UNIQ_1', 'b', NOW())")
        ).hasMessageContaining("duplicate");  // Postgres reports "duplicate key value violates unique constraint"
    }
}
