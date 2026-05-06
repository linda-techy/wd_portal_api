package com.wd.api.service;

import com.wd.api.model.PortalUser;
import com.wd.api.model.ProjectMilestone;
import com.wd.api.model.Task;
import com.wd.api.repository.ProjectMilestoneRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.wbs.ProgressRollupService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TaskProgressUpdateServiceTest {

    private final TaskRepository taskRepo = Mockito.mock(TaskRepository.class);
    private final ProjectMilestoneRepository milestoneRepo = Mockito.mock(ProjectMilestoneRepository.class);
    private final ProgressRollupService rollup = new ProgressRollupService();
    private final ActivityFeedService activityFeed = Mockito.mock(ActivityFeedService.class);
    private final CpmService cpmService = Mockito.mock(CpmService.class);
    private final TaskProgressUpdateService service =
            new TaskProgressUpdateService(taskRepo, milestoneRepo, rollup, activityFeed, cpmService);

    private Task taskWithProgress(int currentProgress, Task.TaskStatus status) {
        Task t = new Task();
        t.setId(1L);
        t.setTitle("Test Task");
        t.setProgressPercent(currentProgress);
        t.setStatus(status);
        return t;
    }

    @Test
    void zeroToFiftyTransitionsPendingToInProgress() {
        Task task = taskWithProgress(0, Task.TaskStatus.PENDING);
        Mockito.when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.updateProgress(1L, 50, "test note", null);

        assertThat(result.getProgressPercent()).isEqualTo(50);
        assertThat(result.getStatus()).isEqualTo(Task.TaskStatus.IN_PROGRESS);
    }

    @Test
    void fiftyToHundredTransitionsToCompletedAndSetsActualEndDate() {
        Task task = taskWithProgress(50, Task.TaskStatus.IN_PROGRESS);
        Mockito.when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(milestoneRepo.findById(any())).thenReturn(Optional.empty());

        Task result = service.updateProgress(1L, 100, null, null);

        assertThat(result.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(result.getActualEndDate()).isNotNull();
    }

    @Test
    void hundredBackToNinetyTransitionsCompletedToInProgress() {
        Task task = taskWithProgress(100, Task.TaskStatus.COMPLETED);
        Mockito.when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.updateProgress(1L, 90, null, null);

        assertThat(result.getStatus()).isEqualTo(Task.TaskStatus.IN_PROGRESS);
    }

    @Test
    void statusTransitionLogsActivityFeedWhenProjectPresent() {
        Task task = taskWithProgress(0, Task.TaskStatus.PENDING);
        com.wd.api.model.CustomerProject project = new com.wd.api.model.CustomerProject();
        task.setProject(project);
        Mockito.when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateProgress(1L, 50, null, null);

        Mockito.verify(activityFeed).logProjectActivity(
                eq("TASK_STATUS_CHANGED"),
                any(String.class),
                any(String.class),
                any(),
                any()
        );
    }

    @Test
    void noStatusTransitionDoesNotLogActivityFeed() {
        Task task = taskWithProgress(50, Task.TaskStatus.IN_PROGRESS);
        Mockito.when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        Mockito.when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateProgress(1L, 60, null, null);

        Mockito.verifyNoInteractions(activityFeed);
    }

    @Test
    void unknownTaskThrowsIllegalArgument() {
        Mockito.when(taskRepo.findById(999L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateProgress(999L, 50, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void invalidProgressThrows() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateProgress(1L, 150, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateProgress(1L, -5, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
