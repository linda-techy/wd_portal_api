package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.TaskBaseline;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.TaskBaselineRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.VarianceRowDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VarianceServiceTest {

    @Mock private ProjectBaselineRepository projectBaselineRepo;
    @Mock private TaskBaselineRepository taskBaselineRepo;
    @Mock private TaskRepository taskRepo;

    private VarianceService service;

    @BeforeEach
    void setUp() {
        service = new VarianceService(projectBaselineRepo, taskBaselineRepo, taskRepo);
    }

    @Test
    void computeFor_noBaseline_throwsNoBaselineException() {
        when(projectBaselineRepo.findByProjectId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.computeFor(42L))
                .isInstanceOf(NoBaselineException.class);
    }

    @Test
    void computeFor_planMatchesBaseline_zeroVariance_noActuals() {
        seedBaseline(42L, 1L);
        Task t = task(101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                null, null, false);
        TaskBaseline tb = taskBaseline(1L, 101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 7);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(t));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of(tb));

        List<VarianceRowDto> rows = service.computeFor(42L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).planVsBaselineDays()).isZero();
        assertThat(rows.get(0).actualVsPlanDays()).isNull();
        assertThat(rows.get(0).actualVsBaselineDays()).isNull();
        assertThat(rows.get(0).isCritical()).isFalse();
    }

    @Test
    void computeFor_planSlipsAheadOfBaseline_negativeVariance() {
        seedBaseline(42L, 1L);
        Task t = task(101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 8),    // 2 days earlier than baseline
                null, null, false);
        TaskBaseline tb = taskBaseline(1L, 101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 7);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(t));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of(tb));

        List<VarianceRowDto> rows = service.computeFor(42L);
        assertThat(rows.get(0).planVsBaselineDays()).isEqualTo(-2);
    }

    @Test
    void computeFor_planSlipsBehindBaseline_positiveVariance_critical() {
        seedBaseline(42L, 1L);
        Task t = task(101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 13),   // 3 days later
                null, null, true);
        TaskBaseline tb = taskBaseline(1L, 101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 7);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(t));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of(tb));

        List<VarianceRowDto> rows = service.computeFor(42L);
        assertThat(rows.get(0).planVsBaselineDays()).isEqualTo(3);
        assertThat(rows.get(0).isCritical()).isTrue();
    }

    @Test
    void computeFor_actualPresent_computesAllThreeDeltas() {
        seedBaseline(42L, 1L);
        Task t = task(101L, "Slab",
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 12),    // plan slipped 2 days
                LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 14),    // actual slipped 2 more
                false);
        TaskBaseline tb = taskBaseline(1L, 101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 7);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(t));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of(tb));

        VarianceRowDto row = service.computeFor(42L).get(0);
        assertThat(row.planVsBaselineDays()).isEqualTo(2);   // 6/12 vs 6/10
        assertThat(row.actualVsPlanDays()).isEqualTo(2);     // 6/14 vs 6/12
        assertThat(row.actualVsBaselineDays()).isEqualTo(4); // 6/14 vs 6/10
    }

    @Test
    void computeFor_actualStartButNoActualEnd_actualDeltasNull() {
        seedBaseline(42L, 1L);
        Task t = task(101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 12),
                LocalDate.of(2026, 6, 1), null,                  // actualEnd missing
                false);
        TaskBaseline tb = taskBaseline(1L, 101L, "Slab",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), 7);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(t));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of(tb));

        VarianceRowDto row = service.computeFor(42L).get(0);
        assertThat(row.planVsBaselineDays()).isEqualTo(2);
        assertThat(row.actualVsPlanDays()).isNull();
        assertThat(row.actualVsBaselineDays()).isNull();
    }

    @Test
    void computeFor_taskAddedAfterBaseline_baselineFieldsNull() {
        seedBaseline(42L, 1L);
        Task added = task(102L, "Late-added",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                null, null, false);
        when(taskRepo.findByProjectId(42L)).thenReturn(List.of(added));
        when(taskBaselineRepo.findByBaselineId(1L)).thenReturn(List.of()); // none for this task

        VarianceRowDto row = service.computeFor(42L).get(0);
        assertThat(row.baselineStart()).isNull();
        assertThat(row.baselineEnd()).isNull();
        assertThat(row.planVsBaselineDays()).isNull();
        assertThat(row.actualVsBaselineDays()).isNull();
    }

    private void seedBaseline(Long projectId, Long baselineId) {
        ProjectBaseline pb = new ProjectBaseline();
        pb.setId(baselineId);
        pb.setProjectId(projectId);
        when(projectBaselineRepo.findByProjectId(projectId)).thenReturn(Optional.of(pb));
    }

    private static Task task(Long id, String title,
                             LocalDate es, LocalDate ef,
                             LocalDate actualStart, LocalDate actualEnd,
                             boolean critical) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setEsDate(es);
        t.setEfDate(ef);
        t.setActualStartDate(actualStart);
        t.setActualEndDate(actualEnd);
        t.setIsCritical(critical);
        return t;
    }

    private static TaskBaseline taskBaseline(Long baselineId, Long taskId, String name,
                                             LocalDate start, LocalDate end, int durationDays) {
        return new TaskBaseline(baselineId, taskId, name, start, end, durationDays);
    }
}
