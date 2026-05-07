package com.wd.api.service.scheduling;

import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.WebhookPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HandoverShiftDetector}. Pure mock-based — verifies
 * threshold semantics, cooldown via {@code lastAlertedHandoverDate}, and
 * baseline fallback on first-ever alert.
 */
@ExtendWith(MockitoExtension.class)
class HandoverShiftDetectorTest {

    @Mock private TaskRepository taskRepo;
    @Mock private ProjectScheduleConfigRepository configRepo;
    @Mock private ProjectBaselineRepository baselineRepo;
    @Mock private HolidayService holidayService;
    @Mock private WebhookPublisherService webhookPublisher;

    @InjectMocks private HandoverShiftDetector detector;

    private static final long PROJECT = 7L;
    private ProjectScheduleConfig cfg;

    @BeforeEach
    void setUp() {
        cfg = new ProjectScheduleConfig();
        cfg.setProjectId(PROJECT);
        cfg.setSundayWorking(false);
        cfg.setLastAlertedHandoverDate(null);
        lenient().when(configRepo.findByProjectId(PROJECT)).thenReturn(Optional.of(cfg));
        lenient().when(holidayService.holidaysFor(eq(PROJECT), any(), any())).thenReturn(Set.of());
    }

    @Test
    void noTasksAtAll_doesNotAlert() {
        when(taskRepo.findMaxEfDateByProjectId(PROJECT)).thenReturn(Optional.empty());

        detector.checkAndAlert(PROJECT);

        verifyNoInteractions(webhookPublisher);
        assertThat(cfg.getLastAlertedHandoverDate()).isNull();
    }

    @Test
    void noBaselineAndNoPriorAlert_doesNotAlert() {
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 15)));
        when(baselineRepo.findByProjectId(PROJECT)).thenReturn(Optional.empty());

        detector.checkAndAlert(PROJECT);

        verifyNoInteractions(webhookPublisher);
        assertThat(cfg.getLastAlertedHandoverDate()).isNull();
    }

    @Test
    void firstAlertAfterBaselineApproved_alertsWhenShiftBeyondThreshold() {
        // Baseline finish = 2026-09-15 (Tue). Current EF = 2026-09-22 (Tue) → 6 working days later
        // (skipping the Sun 2026-09-20).
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 22)));
        ProjectBaseline baseline = new ProjectBaseline(PROJECT,
                java.time.LocalDateTime.now(), 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 15));
        when(baselineRepo.findByProjectId(PROJECT)).thenReturn(Optional.of(baseline));

        detector.checkAndAlert(PROJECT);

        ArgumentCaptor<Integer> shift = ArgumentCaptor.forClass(Integer.class);
        verify(webhookPublisher).publishHandoverShifted(eq(PROJECT),
                eq(LocalDate.of(2026, 9, 15)), eq(LocalDate.of(2026, 9, 22)),
                shift.capture());
        assertThat(shift.getValue()).isEqualTo(6);
        assertThat(cfg.getLastAlertedHandoverDate()).isEqualTo(LocalDate.of(2026, 9, 22));
        verify(configRepo).save(cfg);
    }

    @Test
    void shiftOfExactlyThreeWorkingDays_doesNotAlert() {
        // Baseline 2026-09-15 (Tue) → +3 working days = 2026-09-18 (Fri).
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 18)));
        ProjectBaseline baseline = new ProjectBaseline(PROJECT,
                java.time.LocalDateTime.now(), 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 15));
        when(baselineRepo.findByProjectId(PROJECT)).thenReturn(Optional.of(baseline));

        detector.checkAndAlert(PROJECT);

        verifyNoInteractions(webhookPublisher);
        assertThat(cfg.getLastAlertedHandoverDate()).isNull();
        verify(configRepo, never()).save(any());
    }

    @Test
    void shiftOfFourWorkingDays_alerts() {
        // Baseline 2026-09-15 (Tue) → +4 working days = 2026-09-19 (Sat) (skip nothing in Mon-Sat).
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 19)));
        ProjectBaseline baseline = new ProjectBaseline(PROJECT,
                java.time.LocalDateTime.now(), 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 15));
        when(baselineRepo.findByProjectId(PROJECT)).thenReturn(Optional.of(baseline));

        detector.checkAndAlert(PROJECT);

        verify(webhookPublisher).publishHandoverShifted(eq(PROJECT),
                eq(LocalDate.of(2026, 9, 15)), eq(LocalDate.of(2026, 9, 19)), eq(4));
    }

    @Test
    void negativeShift_handoverEarlier_stillAlerts() {
        // Last alerted = 2026-09-22 (Tue); current = 2026-09-15 (Tue) → 6 working days earlier.
        cfg.setLastAlertedHandoverDate(LocalDate.of(2026, 9, 22));
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 15)));

        detector.checkAndAlert(PROJECT);

        ArgumentCaptor<Integer> shift = ArgumentCaptor.forClass(Integer.class);
        verify(webhookPublisher).publishHandoverShifted(eq(PROJECT),
                eq(LocalDate.of(2026, 9, 22)), eq(LocalDate.of(2026, 9, 15)),
                shift.capture());
        assertThat(shift.getValue()).isEqualTo(-6);
        assertThat(cfg.getLastAlertedHandoverDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void subsequentCpm_comparesAgainstLastAlertedNotBaseline() {
        // Baseline finish = 2026-09-15; lastAlerted = 2026-09-22; current = 2026-09-25 (Fri).
        // Shift from baseline = 9 (would alert), shift from lastAlerted = 3 working days
        // (Wed/Thu/Fri) → at exactly the threshold so must NOT alert.
        cfg.setLastAlertedHandoverDate(LocalDate.of(2026, 9, 22));
        when(taskRepo.findMaxEfDateByProjectId(PROJECT))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 25)));

        detector.checkAndAlert(PROJECT);

        verifyNoInteractions(webhookPublisher);
        // Baseline lookup must be SKIPPED when lastAlerted is present.
        verify(baselineRepo, never()).findByProjectId(anyLong());
    }
}
