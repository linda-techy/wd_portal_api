package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.TaskBaseline;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.TaskBaselineRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.ApproveBaselineResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectBaselineServiceTest {

    @Mock private ProjectBaselineRepository projectBaselineRepo;
    @Mock private TaskBaselineRepository taskBaselineRepo;
    @Mock private TaskRepository taskRepo;
    @Mock private CpmService cpmService;

    private ProjectBaselineService service;

    @BeforeEach
    void setUp() {
        service = new ProjectBaselineService(
                projectBaselineRepo, taskBaselineRepo, taskRepo, cpmService);
    }

    @Test
    void approve_happyPath_recomputesCpmThenSnapshotsTasks() {
        Long projectId = 42L;
        Long approverId = 7L;
        Task t1 = task(101L, "Foundation", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));
        Task t2 = task(102L, "Slab",       LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 20));
        when(taskRepo.findByProjectId(projectId)).thenReturn(List.of(t1, t2));
        when(projectBaselineRepo.existsByProjectId(projectId)).thenReturn(false);
        when(projectBaselineRepo.save(any(ProjectBaseline.class))).thenAnswer(inv -> {
            ProjectBaseline pb = inv.getArgument(0);
            pb.setId(999L);
            return pb;
        });

        ApproveBaselineResponse resp = service.approve(projectId, approverId);

        // CPM recompute MUST happen before any baseline writes (so we capture
        // CPM-computed ES/EF rather than stale plan-only dates).
        InOrder order = inOrder(cpmService, projectBaselineRepo, taskBaselineRepo);
        order.verify(cpmService).recompute(projectId);
        order.verify(projectBaselineRepo).save(any(ProjectBaseline.class));
        order.verify(taskBaselineRepo).saveAll(anyList());

        ArgumentCaptor<List<TaskBaseline>> cap = ArgumentCaptor.forClass(List.class);
        verify(taskBaselineRepo).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(2);
        assertThat(resp.taskCount()).isEqualTo(2);
        assertThat(resp.baselineId()).isEqualTo(999L);
    }

    @Test
    void approve_secondTime_throwsBaselineAlreadyExists() {
        Long projectId = 42L;
        when(projectBaselineRepo.existsByProjectId(projectId)).thenReturn(true);

        assertThatThrownBy(() -> service.approve(projectId, 7L))
                .isInstanceOf(BaselineAlreadyExistsException.class);

        verify(cpmService, never()).recompute(any());
        verify(projectBaselineRepo, never()).save(any());
        verify(taskBaselineRepo, never()).saveAll(any());
    }

    @Test
    void approve_projectWithNoTasks_throwsIllegalState() {
        Long projectId = 42L;
        when(projectBaselineRepo.existsByProjectId(projectId)).thenReturn(false);
        when(taskRepo.findByProjectId(projectId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.approve(projectId, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one task");
    }

    @Test
    void getBaseline_whenAbsent_throwsNoBaselineException() {
        when(projectBaselineRepo.findByProjectId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBaseline(42L))
                .isInstanceOf(NoBaselineException.class);
    }

    private static Task task(Long id, String title, LocalDate es, LocalDate ef) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setEsDate(es);
        t.setEfDate(ef);
        return t;
    }
}
