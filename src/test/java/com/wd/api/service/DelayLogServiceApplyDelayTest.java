package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.Task;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.DelayApplier;
import com.wd.api.service.scheduling.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DelayApplier}: shifts every PENDING
 * task on the project forward by the delay's duration in working days,
 * preserving the task's original working-day duration.
 */
@ExtendWith(MockitoExtension.class)
class DelayLogServiceApplyDelayTest {

    @Mock private TaskRepository taskRepository;
    @Mock private HolidayService holidayService;

    private DelayApplier applier;

    @BeforeEach
    void setUp() {
        applier = new DelayApplier(taskRepository, holidayService, /*sundayWorking*/ false);
        lenient().when(holidayService.holidaysFor(eq(99L), any(), any())).thenReturn(Set.of());
    }

    @Test
    void wholeProjectDelay_shiftsAllPendingTasks_byDurationWorkingDays() {
        CustomerProject p = new CustomerProject();
        p.setId(99L);
        Task pending = task(1L, p, Task.TaskStatus.PENDING,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10));
        Task inProgress = task(2L, p, Task.TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        Task completed = task(3L, p, Task.TaskStatus.COMPLETED,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        when(taskRepository.findByProjectId(99L))
                .thenReturn(List.of(pending, inProgress, completed));

        DelayLog d = DelayLog.builder()
                .project(p)
                .fromDate(LocalDate.of(2026, 7, 6))
                .toDate(LocalDate.of(2026, 7, 10))
                .durationDays(5)
                .build();

        applier.applyDelayToTasks(d);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        // 5 working days from 2026-07-01 (Wed) skipping Sundays.
        // 07-01 Wed (start, day 0) → +1=Thu 07-02 → +2=Fri 07-03 → +3=Sat 07-04
        // → skip Sun 07-05 → +4=Mon 07-06 → +5=Tue 07-07
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        // Original working-day duration:
        // start 07-01 (Wed) → end 07-10 (Fri): 07-02 (1), 07-03 (2), 07-04 (3),
        // skip 07-05 (Sun), 07-06 (4), 07-07 (5), 07-08 (6), 07-09 (7), 07-10 (8).
        // 8 working days from new start 2026-07-07 (Tue):
        // 07-08 (1), 07-09 (2), 07-10 (3), 07-11 (4), skip 07-12 (Sun), 07-13 (5),
        // 07-14 (6), 07-15 (7), 07-16 (8) → Thu.
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 16));
    }

    @Test
    void wholeProjectDelay_skipsTasksWithoutStartOrEnd() {
        CustomerProject p = new CustomerProject();
        p.setId(99L);
        Task noDates = task(1L, p, Task.TaskStatus.PENDING, null, null);
        when(taskRepository.findByProjectId(99L)).thenReturn(List.of(noDates));

        DelayLog d = DelayLog.builder().project(p).durationDays(5).build();
        applier.applyDelayToTasks(d);
        verify(taskRepository).findByProjectId(99L);
        verify(taskRepository, times(0)).save(any());
    }

    @Test
    void wholeProjectDelay_zeroDuration_isNoop() {
        CustomerProject p = new CustomerProject();
        p.setId(99L);
        DelayLog d = DelayLog.builder().project(p).durationDays(0).build();
        applier.applyDelayToTasks(d);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void nullDuration_isNoop() {
        CustomerProject p = new CustomerProject();
        p.setId(99L);
        DelayLog d = DelayLog.builder().project(p).durationDays(null).build();
        applier.applyDelayToTasks(d);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void nullProject_isNoop() {
        DelayLog d = DelayLog.builder().durationDays(5).build();
        applier.applyDelayToTasks(d);
        verifyNoInteractions(taskRepository);
    }

    private Task task(Long id, CustomerProject p, Task.TaskStatus s,
                      LocalDate start, LocalDate end) {
        Task t = new Task();
        t.setId(id);
        t.setProject(p);
        t.setStatus(s);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setDueDate(end != null ? end : LocalDate.now());
        return t;
    }
}
