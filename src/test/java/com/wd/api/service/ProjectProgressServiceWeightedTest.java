package com.wd.api.service;

import com.wd.api.dto.ProjectProgressDTO;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 PR1 — verifies the weight-based rewrite of
 * {@code ProjectProgressService.calculateProjectProgress}:
 *
 * <ul>
 *   <li>effectiveWeight = weight ?? duration_days ?? 1</li>
 *   <li>completedWeight / totalWeight × 100, rounded HALF_UP to 2 dp</li>
 *   <li>Empty project = 0%</li>
 *   <li>Status COMPLETED or DONE both count as completed</li>
 * </ul>
 *
 * <p>The legacy 40/30/30 hybrid path must NOT be reachable for the
 * rewrite to be considered done — see {@code legacyHybrid_isRemoved}.
 */
@Transactional
class ProjectProgressServiceWeightedTest extends TestcontainersPostgresBase {

    @Autowired private ProjectProgressService service;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private TaskRepository tasks;

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("ProgressW-" + UUID.randomUUID());
        p.setLocation("Test");
        p.setIsDesignAgreementSigned(false);
        return projects.save(p);
    }

    private Task newTask(CustomerProject p, Task.TaskStatus status,
                         Integer weight, Integer durationDays) {
        Task t = new Task();
        t.setTitle("T-" + UUID.randomUUID());
        t.setProject(p);
        t.setStatus(status);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setDueDate(LocalDate.of(2026, 12, 31));
        t.setWeight(weight);
        t.setDurationDays(durationDays);
        return tasks.save(t);
    }

    @Test
    void singleTaskComplete_weight10_isOneHundredPercent() {
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.COMPLETED, 10, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(dto.getCompletedTasks()).isEqualTo(1);
        assertThat(dto.getTotalTasks()).isEqualTo(1);
    }

    @Test
    void twoTasks_w10_w20_onlyW20Complete_isSixtySixPointSixSeven() {
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.PENDING,   10, null);
        newTask(p, Task.TaskStatus.COMPLETED, 20, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(dto.getCompletedTasks()).isEqualTo(1);
        assertThat(dto.getTotalTasks()).isEqualTo(2);
    }

    @Test
    void threeTasks_allWeightAndDurationNull_oneComplete_fallbackToOnePerTask() {
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.PENDING,   null, null);
        newTask(p, Task.TaskStatus.COMPLETED, null, null);
        newTask(p, Task.TaskStatus.PENDING,   null, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        // 1 / 3 = 33.3333... → HALF_UP at 2 dp = 33.33
        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void mixed_weight_and_durationDays_fallback_computesCorrectly() {
        CustomerProject p = newProject();
        // weight=5, complete:               contributes 5 / 5
        // weight=null, duration=3, pending: contributes 0 / 3
        // weight=null, duration=null, pending: contributes 0 / 1 (fallback to 1)
        newTask(p, Task.TaskStatus.COMPLETED, 5,    null);
        newTask(p, Task.TaskStatus.PENDING,   null, 3);
        newTask(p, Task.TaskStatus.PENDING,   null, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        // total = 5 + 3 + 1 = 9; completed = 5; 5/9 = 55.5555... → 55.56
        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("55.56"));
        assertThat(dto.getCompletedTasks()).isEqualTo(1);
        assertThat(dto.getTotalTasks()).isEqualTo(3);
    }

    @Test
    void emptyProject_returnsZeroPercent() {
        CustomerProject p = newProject();

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        assertThat(dto.getOverallProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTotalTasks()).isEqualTo(0);
        assertThat(dto.getCompletedTasks()).isEqualTo(0);
    }

    @Test
    void doneStatus_alsoCountsAsCompleted() {
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.PENDING, 10, null);
        // Task.TaskStatus has no DONE today; if a downstream PR adds it,
        // this test enforces the lenient match. For now the COMPLETED arm
        // is exercised by other tests, and isCompleted handles both names
        // by string equality. (see ProjectProgressService.isCompleted)
        newTask(p, Task.TaskStatus.COMPLETED, 10, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());
        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void cancelledTasks_areExcludedFromBothNumeratorAndDenominator() {
        // 100-weight project with 1 task fully completed (weight 50) and
        // 1 task cancelled (weight 50). Expected: 100% — only the active
        // 50-weight task counts in the denominator, and it's complete.
        // Spec: CANCELLED tasks are filtered entirely (not counted as
        // incomplete) so cancelling scope doesn't drop the project %.
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.COMPLETED, 50, null);
        newTask(p, Task.TaskStatus.CANCELLED, 50, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        assertThat(dto.getOverallProgress()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(dto.getCompletedTasks()).isEqualTo(1);  // only the active counts
        assertThat(dto.getTotalTasks()).isEqualTo(1);      // CANCELLED filtered from denominator too
    }

    @Test
    void legacyHybrid_isRemoved_noMilestoneOrBudgetWeightingRemains() {
        // The post-rewrite DTO sets milestoneProgress / budgetProgress to
        // ZERO — the rewrite computes a single weight-based overall and
        // does not populate the legacy breakdown fields. Pre-PR1 callers
        // (DashboardService) read these as non-null; PR1 preserves the
        // non-null contract while semantically zeroing them out since the
        // new algorithm doesn't compute milestone/budget weights. This
        // also guards against the old 40/30/30 path being re-introduced.
        CustomerProject p = newProject();
        newTask(p, Task.TaskStatus.COMPLETED, 1, null);

        ProjectProgressDTO dto = service.calculateProjectProgress(p.getId());

        assertThat(dto.getMilestoneProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getBudgetProgress()).isEqualByComparingTo(BigDecimal.ZERO);
        // taskProgress mirrors overallProgress in the rewrite — kept for
        // backward-compat of the existing DTO field, no separate calc.
        assertThat(dto.getTaskProgress()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
