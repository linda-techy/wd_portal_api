package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.scheduling.HandoverShiftDetector;
import com.wd.api.service.scheduling.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link DelayLogService#logDelay} and
 * {@link DelayLogService#closeDelay} both invoke the S3 PR3 hook chain
 * (apply delay → CPM recompute → handover shift detector) in the correct
 * order, and that the existing {@code publishDelayReported} webhook still
 * fires from {@code logDelay}.
 */
@ExtendWith(MockitoExtension.class)
class DelayLogServiceCpmHookTest {

    @Mock private DelayLogRepository delayLogRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private WebhookPublisherService webhookPublisherService;
    @Mock private TaskRepository taskRepository;
    @Mock private HolidayService holidayService;
    @Mock private CpmService cpmService;
    @Mock private HandoverShiftDetector handoverShiftDetector;
    @Mock private ProjectScheduleConfigRepository scheduleConfigRepo;

    @InjectMocks private DelayLogService service;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(11L);
        lenient().when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        lenient().when(taskRepository.findByProjectId(11L)).thenReturn(Collections.emptyList());
        lenient().when(scheduleConfigRepo.findByProjectId(11L)).thenReturn(Optional.empty());
        lenient().when(delayLogRepository.save(any(DelayLog.class)))
                .thenAnswer(inv -> { DelayLog d = inv.getArgument(0); d.setId(7L); return d; });
    }

    @Test
    void logDelay_invokesCpmRecomputeThenHandoverShiftDetector_inOrder() {
        DelayLog d = DelayLog.builder()
                .delayType("WEATHER")
                .fromDate(LocalDate.of(2026, 7, 1))
                .toDate(LocalDate.of(2026, 7, 5))
                .durationDays(5)
                .build();

        service.logDelay(d, 11L, null);

        InOrder ord = inOrder(cpmService, handoverShiftDetector);
        ord.verify(cpmService).recompute(11L);
        ord.verify(handoverShiftDetector).checkAndAlert(11L);
    }

    @Test
    void logDelay_alsoFiresExistingDelayReportedWebhook() {
        DelayLog d = DelayLog.builder()
                .delayType("WEATHER")
                .reasonCategory("WEATHER")
                .fromDate(LocalDate.of(2026, 7, 1))
                .toDate(LocalDate.of(2026, 7, 5))
                .durationDays(5)
                .build();

        service.logDelay(d, 11L, null);
        verify(webhookPublisherService).publishDelayReported(11L, 7L, "WEATHER");
    }

    @Test
    void closeDelay_alsoTriggersCpmRecomputeAndAlert() {
        DelayLog existing = DelayLog.builder()
                .id(7L)
                .project(project)
                .delayType("WEATHER")
                .fromDate(LocalDate.of(2026, 7, 1))
                .durationDays(null)
                .build();
        when(delayLogRepository.findById(7L)).thenReturn(Optional.of(existing));

        service.closeDelay(7L, LocalDate.of(2026, 7, 8));

        InOrder ord = inOrder(cpmService, handoverShiftDetector);
        ord.verify(cpmService).recompute(11L);
        ord.verify(handoverShiftDetector).checkAndAlert(11L);
    }

    /**
     * Regression: when a delay was created with a non-null {@code durationDays}
     * (the typical UI flow — POST /delay-logs with durationDays=5), {@code logDelay}
     * already shifted every PENDING task forward by that many working days.
     * {@code closeDelay} must NOT shift them again — re-applying the same
     * duration on the same row is a silent double-shift bug.
     */
    @Test
    void closeDelay_doesNotDoubleShiftPendingTasks() {
        DelayLog existing = DelayLog.builder()
                .id(99L)
                .project(project)
                .delayType("WEATHER")
                .fromDate(LocalDate.of(2026, 6, 1))
                .durationDays(5)  // non-null — this is the bug trigger
                .build();
        when(delayLogRepository.findById(99L)).thenReturn(Optional.of(existing));

        Task pendingTask = new Task();
        pendingTask.setId(1L);
        pendingTask.setProject(project);
        pendingTask.setStatus(Task.TaskStatus.PENDING);
        pendingTask.setStartDate(LocalDate.of(2026, 6, 15));
        pendingTask.setEndDate(LocalDate.of(2026, 6, 20));
        // The applier (if it were invoked) would look up tasks via
        // taskRepository.findByProjectId(projectId). Stub a non-empty list
        // leniently — the whole point of this test is to assert the applier
        // is NOT invoked, so this stub is intentionally unused after the fix.
        lenient().when(taskRepository.findByProjectId(11L)).thenReturn(List.of(pendingTask));

        service.closeDelay(99L, LocalDate.of(2026, 6, 6));

        // closeDelay must NOT re-apply the delay duration to the task.
        verify(taskRepository, never()).save(any(Task.class));
        // CPM recompute IS still called (independent change driver).
        verify(cpmService).recompute(11L);
        verify(handoverShiftDetector).checkAndAlert(11L);
    }
}
