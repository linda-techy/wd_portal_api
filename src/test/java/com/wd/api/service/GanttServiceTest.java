package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GanttService.
 */
@ExtendWith(MockitoExtension.class)
class GanttServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskPredecessorRepository taskPredecessorRepository;

    @Mock
    private com.wd.api.service.scheduling.CpmService cpmService;

    @InjectMocks
    private GanttService ganttService;

    // ── Helper ────────────────────────────────────────────────────────────────

    private Task task(Long id, Task.TaskStatus status, LocalDate start, LocalDate end, int progress) {
        Task t = new Task();
        t.setId(id);
        t.setTitle("Task " + id);
        t.setStatus(status);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setDueDate(end != null ? end : LocalDate.now().plusDays(7));
        t.setProgressPercent(progress);
        return t;
    }

    // ── getGanttData ─────────────────────────────────────────────────────────

    @Test
    void getGanttData_returnsTasksOrderedByRepository() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 3, 31);

        Task t1 = task(1L, Task.TaskStatus.IN_PROGRESS, start, end, 50);
        Task t2 = task(2L, Task.TaskStatus.PENDING, start.plusMonths(1), end.plusMonths(1), 0);

        when(taskRepository.findByProjectIdOrderedForGantt(1L)).thenReturn(List.of(t1, t2));

        Map<String, Object> result = ganttService.getGanttData(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).get("id")).isEqualTo(1L);
        assertThat(tasks.get(1).get("id")).isEqualTo(2L);
    }

    @Test
    void getGanttData_calculatesOverallProgressCorrectly() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end   = LocalDate.now().plusDays(10);

        Task t1 = task(1L, Task.TaskStatus.IN_PROGRESS, start, end, 60);
        Task t2 = task(2L, Task.TaskStatus.IN_PROGRESS, start, end, 40);

        when(taskRepository.findByProjectIdOrderedForGantt(1L)).thenReturn(List.of(t1, t2));

        Map<String, Object> result = ganttService.getGanttData(1L);

        // average of 60 and 40 = 50
        assertThat(result.get("overallProgress")).isEqualTo(50);
    }

    @Test
    void getGanttData_excludesCancelledTasksFromProgressCalculation() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end   = LocalDate.now().plusDays(5);

        Task active    = task(1L, Task.TaskStatus.IN_PROGRESS, start, end, 80);
        Task cancelled = task(2L, Task.TaskStatus.CANCELLED, start, end, 0);

        when(taskRepository.findByProjectIdOrderedForGantt(1L)).thenReturn(List.of(active, cancelled));

        Map<String, Object> result = ganttService.getGanttData(1L);

        // Only active task counts: progress = 80
        assertThat(result.get("overallProgress")).isEqualTo(80);
    }

    @Test
    void getGanttData_identifiesOverdueTasks() {
        // Overdue: endDate in the past, not completed/cancelled
        Task overdue  = task(1L, Task.TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(20), LocalDate.now().minusDays(5), 30);
        Task onTrack  = task(2L, Task.TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(10), 50);

        when(taskRepository.findByProjectIdOrderedForGantt(1L)).thenReturn(List.of(overdue, onTrack));

        Map<String, Object> result = ganttService.getGanttData(1L);

        assertThat(result.get("overdueTasks")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
        assertThat(tasks.get(0).get("overdue")).isEqualTo(true);
        assertThat(tasks.get(1).get("overdue")).isEqualTo(false);
    }

    // ── updateTaskSchedule ────────────────────────────────────────────────────

    @Test
    void updateTaskSchedule_endDateBeforeStartDate_throwsIllegalArgumentException() {
        Task task = task(1L, Task.TaskStatus.PENDING,
                LocalDate.now(), LocalDate.now().plusDays(10), 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> ganttService.updateTaskSchedule(
                1L,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(2), // before start
                0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate must be on or after startDate");
    }

    @Test
    void updateTaskSchedule_progressOver100_throwsIllegalArgumentException() {
        Task task = task(1L, Task.TaskStatus.IN_PROGRESS,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), 50);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> ganttService.updateTaskSchedule(
                1L, null, null, 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("progressPercent must be between 0 and 100");
    }

    // S2 PR2 dropped Task.dependsOnTaskId. Cycle detection now lives in
    // TaskGraphValidator and is exercised through TaskPredecessorService /
    // TaskPredecessorServiceTest — predecessor edits do NOT flow through
    // GanttService.updateTaskSchedule any more. The legacy circular-dep
    // test case has been removed accordingly.

    @Test
    void updateTaskSchedule_validUpdate_savesTask() {
        Task task = task(1L, Task.TaskStatus.PENDING, null, null, 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newStart = LocalDate.now();
        LocalDate newEnd   = LocalDate.now().plusDays(30);

        Task result = ganttService.updateTaskSchedule(1L, newStart, newEnd, 25);

        assertThat(result.getStartDate()).isEqualTo(newStart);
        assertThat(result.getEndDate()).isEqualTo(newEnd);
        assertThat(result.getProgressPercent()).isEqualTo(25);
        verify(taskRepository).save(task);
    }
}
