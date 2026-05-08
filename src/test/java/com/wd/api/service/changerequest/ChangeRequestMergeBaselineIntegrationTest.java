package com.wd.api.service.changerequest;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.Task;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies S4 PR2 baseline preservation: ChangeRequestMergeService inserts
 * new tasks into the WBS but never touches {@code task_baseline}. The CR-
 * merged tasks therefore appear as scope creep in Plan-vs-Baseline variance
 * reports.
 */
@Transactional
class ChangeRequestMergeBaselineIntegrationTest extends TestcontainersPostgresBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChangeRequestMergeService mergeService;
    @Autowired private CustomerProjectRepository projectRepo;
    @Autowired private ProjectVariationRepository crRepo;
    @Autowired private TaskRepository taskRepo;
    @Autowired private ChangeRequestTaskRepository crTaskRepo;

    @Test
    void merge_doesNotMutateTaskBaseline() {
        // Arrange — project, anchor task, baseline, approved CR with proposed tasks.
        CustomerProject project = new CustomerProject();
        project.setName("baseline-merge-" + UUID.randomUUID());
        project.setLocation("Loc");
        project.setProjectUuid(UUID.randomUUID());
        project.setFloors(1);
        project = projectRepo.save(project);
        Long projectId = project.getId();

        Task anchor = new Task();
        anchor.setTitle("anchor");
        anchor.setStatus(Task.TaskStatus.PENDING);
        anchor.setPriority(Task.TaskPriority.MEDIUM);
        anchor.setProject(project);
        anchor.setDueDate(LocalDate.of(2026, 6, 1));
        anchor = taskRepo.save(anchor);
        Long anchorTaskId = anchor.getId();

        // Seed an approved baseline + a single task_baseline row.
        Long baselineId = jdbc.queryForObject(
                "INSERT INTO project_baseline (project_id, approved_at, approved_by, " +
                "project_start_date, project_finish_date, " +
                "created_at, updated_at, version) " +
                "VALUES (?, NOW(), 1, '2026-01-01', '2026-09-15', NOW(), NOW(), 1) RETURNING id",
                Long.class, projectId);
        jdbc.update(
                "INSERT INTO task_baseline (baseline_id, task_id, task_name_at_baseline, " +
                "baseline_start, baseline_end, baseline_duration_days, " +
                "created_at, updated_at, version) " +
                "VALUES (?, ?, 'anchor', '2026-01-01', '2026-09-15', 270, NOW(), NOW(), 1)",
                baselineId, anchorTaskId);

        ProjectVariation cr = ProjectVariation.builder()
                .description("CR for baseline test")
                .estimatedAmount(new BigDecimal("0"))
                .status(VariationStatus.APPROVED)
                .timeImpactWorkingDays(0)
                .build();
        cr.setProject(project);
        cr = crRepo.save(cr);
        Long crId = cr.getId();

        for (int i = 1; i <= 3; i++) {
            ChangeRequestTask t = new ChangeRequestTask();
            t.setName("Proposed " + i);
            t.setSequence(i);
            t.setDurationDays(1);
            t.setChangeRequest(cr);
            crTaskRepo.save(t);
        }

        Integer baselineCountBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_baseline tb JOIN project_baseline pb " +
                "ON tb.baseline_id = pb.id WHERE pb.project_id = ?",
                Integer.class, projectId);
        Integer taskCountBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE project_id = ?",
                Integer.class, projectId);

        // Act
        mergeService.mergeIntoWbs(crId, anchorTaskId, /*actorUserId*/ 1L);

        // Assert: task_baseline unchanged.
        Integer baselineCountAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_baseline tb JOIN project_baseline pb " +
                "ON tb.baseline_id = pb.id WHERE pb.project_id = ?",
                Integer.class, projectId);
        assertThat(baselineCountAfter).isEqualTo(baselineCountBefore);

        // Sanity: the merge did insert new tasks.
        Integer taskCountAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE project_id = ?",
                Integer.class, projectId);
        assertThat(taskCountAfter).isEqualTo(taskCountBefore + 3);
    }
}
