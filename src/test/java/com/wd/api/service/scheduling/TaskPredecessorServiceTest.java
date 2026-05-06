package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskPredecessorServiceTest {

    @Mock private TaskPredecessorRepository predecessorRepo;
    @Mock private TaskRepository taskRepo;
    @Mock private CpmService cpmService;
    @InjectMocks private TaskPredecessorService service;

    private Task task(long id) {
        Task t = new Task();
        t.setId(id);
        return t;
    }

    @BeforeEach
    void stubLoad() {
        lenient().when(taskRepo.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(task(inv.getArgument(0))));
    }

    @Test
    void replacePredecessors_withSinglePred_persistsEdge() {
        lenient().when(predecessorRepo.findBySuccessorId(anyLong())).thenReturn(List.of());
        when(predecessorRepo.save(any(TaskPredecessor.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replacePredecessors(2L, List.of(new TaskPredecessorService.PredecessorEntry(1L, 0)));

        verify(predecessorRepo).deleteBySuccessorId(2L);
        verify(predecessorRepo).save(any(TaskPredecessor.class));
        // S2 PR2 dropped Task.dependsOnTaskId; the service no longer dual-writes
        // the legacy column. task_predecessor is the canonical edge store.
        verify(taskRepo, never()).save(any());
    }

    @Test
    void replacePredecessors_withZeroPreds_clearsExistingEdges() {
        service.replacePredecessors(2L, List.of());

        verify(predecessorRepo).deleteBySuccessorId(2L);
        verify(predecessorRepo, never()).save(any(TaskPredecessor.class));
        verify(taskRepo, never()).save(any());
    }

    @Test
    void replacePredecessors_withTwoPreds_persistsBothEdges() {
        lenient().when(predecessorRepo.findBySuccessorId(anyLong())).thenReturn(List.of());
        when(predecessorRepo.save(any(TaskPredecessor.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replacePredecessors(3L, List.of(
                new TaskPredecessorService.PredecessorEntry(1L, 0),
                new TaskPredecessorService.PredecessorEntry(2L, 0)));

        verify(predecessorRepo).deleteBySuccessorId(3L);
        verify(predecessorRepo, times(2)).save(any(TaskPredecessor.class));
        verify(taskRepo, never()).save(any());
    }

    @Test
    void replacePredecessors_rejectsSelfLoop() {
        assertThatThrownBy(() ->
                service.replacePredecessors(5L, List.of(
                        new TaskPredecessorService.PredecessorEntry(5L, 0))))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
        // Pin the validate-first / mutate-never contract: a refactor that
        // moved deleteBySuccessorId(taskId) ahead of the cycle check would
        // silently destroy production rows while these tests still passed
        // without these assertions.
        verify(predecessorRepo, never()).save(any());
        verify(predecessorRepo, never()).deleteBySuccessorId(anyLong());
        verify(taskRepo, never()).save(any());
    }

    @Test
    void replacePredecessors_rejectsCycle() {
        // Existing: 2 <- 1, 3 <- 2. Asking to add 1 <- 3 closes the cycle.
        lenient().when(predecessorRepo.findBySuccessorId(1L)).thenReturn(List.of());
        lenient().when(predecessorRepo.findBySuccessorId(2L)).thenReturn(
                List.of(new TaskPredecessor(2L, 1L, 0)));
        lenient().when(predecessorRepo.findBySuccessorId(3L)).thenReturn(
                List.of(new TaskPredecessor(3L, 2L, 0)));

        assertThatThrownBy(() ->
                service.replacePredecessors(1L, List.of(
                        new TaskPredecessorService.PredecessorEntry(3L, 0))))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
        verify(predecessorRepo, never()).save(any());
        verify(predecessorRepo, never()).deleteBySuccessorId(anyLong());
        verify(taskRepo, never()).save(any());
    }
}
