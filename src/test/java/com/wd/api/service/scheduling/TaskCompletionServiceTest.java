package com.wd.api.service.scheduling;

import com.wd.api.dto.scheduling.ProjectScheduleConfigDto;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.model.enums.ReportType;
import com.wd.api.repository.SiteReportRepository;
import com.wd.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-tests the {@link TaskCompletionService} FSM:
 *
 * <pre>
 *   IN_PROGRESS ──markComplete()──▶ COMPLETED          (requires_pm_approval=false)
 *   IN_PROGRESS ──markComplete()──▶ PENDING_PM_APPROVAL (requires_pm_approval=true)
 *   PENDING_PM_APPROVAL ──approveCompletion()──▶ COMPLETED
 *   PENDING_PM_APPROVAL ──rejectCompletion(reason)──▶ IN_PROGRESS
 * </pre>
 *
 * Verifies cpmService.recompute is called AFTER taskRepo.save on every
 * transition (Mockito InOrder).
 */
class TaskCompletionServiceTest {

    private TaskRepository taskRepo;
    private SiteReportRepository siteReportRepo;
    private ProjectScheduleConfigService configService;
    private CpmService cpmService;
    private TaskCompletionService service;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        taskRepo = mock(TaskRepository.class);
        siteReportRepo = mock(SiteReportRepository.class);
        configService = mock(ProjectScheduleConfigService.class);
        cpmService = mock(CpmService.class);
        service = new TaskCompletionService(taskRepo, siteReportRepo, configService, cpmService);

        project = new CustomerProject();
        project.setId(7L);
    }

    private Task task(Task.TaskStatus status) {
        Task t = new Task();
        t.setId(42L);
        t.setStatus(status);
        t.setProject(project);
        return t;
    }

    private void stubGeotaggedPhoto(boolean present) {
        when(siteReportRepo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                eq(42L), eq(ReportType.COMPLETION))).thenReturn(present);
    }

    private void stubRequiresPmApproval(boolean v) {
        when(configService.get(7L)).thenReturn(
                new ProjectScheduleConfigDto(7L, false, (short) 601, (short) 930, null, v));
    }

    // ----- markComplete -----

    @Test
    void markComplete_fromInProgress_withoutPhoto_throws() {
        Task t = task(Task.TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));
        stubGeotaggedPhoto(false);
        stubRequiresPmApproval(false);

        assertThatThrownBy(() -> service.markComplete(42L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("photo");
        verify(taskRepo, never()).save(any());
        verify(cpmService, never()).recompute(anyLong());
    }

    @Test
    void markComplete_notInProgress_throws() {
        Task t = task(Task.TaskStatus.PENDING);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.markComplete(42L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void markComplete_withPhoto_noApprovalRequired_transitionsToCompleted() {
        Task t = task(Task.TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeotaggedPhoto(true);
        stubRequiresPmApproval(false);

        Task out = service.markComplete(42L, 99L);

        assertThat(out.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(out.getActualEndDate()).isEqualTo(LocalDate.now());
        InOrder o = inOrder(taskRepo, cpmService);
        o.verify(taskRepo).save(t);
        o.verify(cpmService).recompute(7L);
    }

    @Test
    void markComplete_withPhoto_approvalRequired_transitionsToPending() {
        Task t = task(Task.TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeotaggedPhoto(true);
        stubRequiresPmApproval(true);

        Task out = service.markComplete(42L, 99L);

        assertThat(out.getStatus()).isEqualTo(Task.TaskStatus.PENDING_PM_APPROVAL);
        assertThat(out.getActualEndDate()).isEqualTo(LocalDate.now());
        InOrder o = inOrder(taskRepo, cpmService);
        o.verify(taskRepo).save(t);
        o.verify(cpmService).recompute(7L);
    }

    // ----- approveCompletion -----

    @Test
    void approveCompletion_fromPending_transitionsToCompletedAndClearsRejection() {
        Task t = task(Task.TaskStatus.PENDING_PM_APPROVAL);
        t.setRejectionReason("earlier rejection");
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task out = service.approveCompletion(42L, 99L);

        assertThat(out.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
        assertThat(out.getRejectionReason()).isNull();
        InOrder o = inOrder(taskRepo, cpmService);
        o.verify(taskRepo).save(t);
        o.verify(cpmService).recompute(7L);
    }

    @Test
    void approveCompletion_fromInProgress_throws() {
        Task t = task(Task.TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.approveCompletion(42L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending approval");
        verify(cpmService, never()).recompute(anyLong());
    }

    // ----- rejectCompletion -----

    @Test
    void rejectCompletion_withReason_bouncesToInProgress() {
        Task t = task(Task.TaskStatus.PENDING_PM_APPROVAL);
        t.setActualEndDate(LocalDate.now());
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));
        when(taskRepo.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task out = service.rejectCompletion(42L, 99L, "Photo blurry, recapture");

        assertThat(out.getStatus()).isEqualTo(Task.TaskStatus.IN_PROGRESS);
        assertThat(out.getActualEndDate()).isNull();
        assertThat(out.getRejectionReason()).isEqualTo("Photo blurry, recapture");
        InOrder o = inOrder(taskRepo, cpmService);
        o.verify(taskRepo).save(t);
        o.verify(cpmService).recompute(7L);
    }

    @Test
    void rejectCompletion_withoutReason_throws() {
        // Intentionally do NOT stub findById — service must short-circuit on
        // blank reason before touching the repo.
        assertThatThrownBy(() -> service.rejectCompletion(42L, 99L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectCompletion_fromInProgress_throws() {
        Task t = task(Task.TaskStatus.IN_PROGRESS);
        when(taskRepo.findById(42L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.rejectCompletion(42L, 99L, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending approval");
    }
}
