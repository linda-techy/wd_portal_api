package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for DelayLogService.
 */
@ExtendWith(MockitoExtension.class)
class DelayLogServiceTest {

    @Mock private DelayLogRepository delayLogRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private WebhookPublisherService webhookPublisherService;

    @InjectMocks
    private DelayLogService delayLogService;

    private CustomerProject project;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(1L);
        project.setName("Highway Extension");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private DelayLog buildDelay(String delayType, String reasonCategory,
                                LocalDate from, LocalDate to) {
        DelayLog d = new DelayLog();
        d.setDelayType(delayType);
        d.setReasonCategory(reasonCategory);
        d.setFromDate(from);
        d.setToDate(to);
        return d;
    }

    // ── logDelay ──────────────────────────────────────────────────────────────

    @Test
    void logDelay_validInput_savesWithProjectAndUser() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        PortalUser user = new PortalUser();
        user.setId(5L);
        when(portalUserRepository.findById(5L)).thenReturn(Optional.of(user));

        DelayLog delay = buildDelay("WEATHER", "WEATHER", LocalDate.now(), null);
        delay.setId(99L);
        when(delayLogRepository.save(any())).thenReturn(delay);

        DelayLog result = delayLogService.logDelay(delay, 1L, 5L);

        assertThat(result).isNotNull();
        verify(delayLogRepository).save(delay);
        assertThat(delay.getProject()).isEqualTo(project);
        assertThat(delay.getLoggedBy()).isEqualTo(user);
        assertThat(delay.getReportedBy()).isEqualTo(5L);
    }

    @Test
    void logDelay_publishesWebhookEvent() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(portalUserRepository.findById(anyLong())).thenReturn(Optional.empty());

        DelayLog delay = buildDelay("MATERIAL_DELAY", "MATERIAL_SHORTAGE",
                LocalDate.now(), null);
        delay.setId(10L);
        when(delayLogRepository.save(any())).thenReturn(delay);

        delayLogService.logDelay(delay, 1L, 1L);

        verify(webhookPublisherService).publishDelayReported(eq(1L), eq(10L), anyString());
    }

    @Test
    void logDelay_nullProjectId_throwsIllegalArgumentException() {
        DelayLog delay = buildDelay("WEATHER", null, LocalDate.now(), null);

        assertThatThrownBy(() -> delayLogService.logDelay(delay, null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project ID cannot be null");
    }

    // ── getDelaysByProject ────────────────────────────────────────────────────

    @Test
    void getDelaysByProject_validProject_returnsAllDelays() {
        DelayLog d1 = buildDelay("WEATHER", null, LocalDate.now().minusDays(5), LocalDate.now().minusDays(3));
        DelayLog d2 = buildDelay("LABOUR_STRIKE", null, LocalDate.now().minusDays(2), null);
        when(delayLogRepository.findByProjectIdOrderByFromDateDesc(1L)).thenReturn(List.of(d1, d2));

        List<DelayLog> result = delayLogService.getDelaysByProject(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getDelaysByProject_nullProjectId_returnsEmptyList() {
        List<DelayLog> result = delayLogService.getDelaysByProject(null);

        assertThat(result).isEmpty();
        verify(delayLogRepository, never()).findByProjectIdOrderByFromDateDesc(any());
    }

    // ── getDelaySummary ───────────────────────────────────────────────────────

    @Test
    void getDelaySummary_aggregatesByCategory() {
        // Two delays: one WEATHER (3 days), one MATERIAL_SHORTAGE (5 days)
        DelayLog d1 = buildDelay("WEATHER", "WEATHER",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));
        DelayLog d2 = buildDelay("MATERIAL_DELAY", "MATERIAL_SHORTAGE",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5));

        when(delayLogRepository.findByProjectId(1L)).thenReturn(List.of(d1, d2));

        Map<String, Object> summary = delayLogService.getDelaySummary(1L);

        assertThat(summary.get("totalDelays")).isEqualTo(2L);
        // d1: Jan 1–3 inclusive = 3 days; d2: Feb 1–5 inclusive = 5 days; total = 8
        assertThat(summary.get("totalDaysLost")).isEqualTo(8L);

        @SuppressWarnings("unchecked")
        Map<String, Long> breakdown = (Map<String, Long>) summary.get("breakdownByCategory");
        assertThat(breakdown).containsEntry("WEATHER", 1L)
                             .containsEntry("MATERIAL_SHORTAGE", 1L);
    }

    @Test
    void getDelaySummary_nullProjectId_returnsEmptyMap() {
        Map<String, Object> result = delayLogService.getDelaySummary(null);

        assertThat(result).isEmpty();
    }
}
