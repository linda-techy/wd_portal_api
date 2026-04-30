package com.wd.api.service;

import com.wd.api.dto.SiteReportActivityRequest;
import com.wd.api.model.SiteReport;
import com.wd.api.model.SiteReportActivity;
import com.wd.api.repository.SiteReportActivityRepository;
import com.wd.api.repository.SiteReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD slice for {@link SiteReportActivityService}.
 *
 * <p>Models: a single site report can carry many concurrent activities —
 * "RCC slab pour" with 8 men, "Plastering" with 4, "Electrical conduit"
 * with 2 — instead of a single rolled-up manpower count. The Portal app
 * captures each row separately; the customer app sees the breakdown plus
 * a derived total.
 *
 * <p>Service responsibilities verified here:
 *
 * <ol>
 *   <li>Replace-all semantics — staff submits the full list each save;
 *       service deletes old rows for that report and inserts the new
 *       set (cleaner than diff-based PUT/DELETE for an MVP).</li>
 *   <li>Validates each request: non-blank name, manpower &ge; 0.</li>
 *   <li>Derives a roll-up {@code totalManpower} for surfaces that still
 *       want a single number.</li>
 *   <li>Rejects unknown report ids with IllegalArgumentException.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SiteReportActivityServiceTest {

    @Mock private SiteReportRepository siteReportRepository;
    @Mock private SiteReportActivityRepository activityRepository;

    private SiteReportActivityService service;

    @BeforeEach
    void setUp() {
        service = new SiteReportActivityService(siteReportRepository, activityRepository);

        SiteReport report = new SiteReport();
        ReflectionTestUtils.setField(report, "id", 7L);
        lenient().when(siteReportRepository.findById(7L)).thenReturn(Optional.of(report));

        lenient().when(activityRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void replaceActivities_deletesExistingThenInsertsNewSet() {
        List<SiteReportActivityRequest> requests = List.of(
                new SiteReportActivityRequest("RCC slab pour", 8, "JCB", null),
                new SiteReportActivityRequest("Plastering", 4, null, "block C"));

        List<SiteReportActivity> saved = service.replaceActivities(7L, requests);

        // Old rows wiped before new rows go in
        verify(activityRepository).deleteByReportId(7L);
        // Both rows persisted
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getName()).isEqualTo("RCC slab pour");
        assertThat(saved.get(0).getManpower()).isEqualTo(8);
        assertThat(saved.get(1).getName()).isEqualTo("Plastering");
        assertThat(saved.get(1).getManpower()).isEqualTo(4);
        // Display order applied 0..n-1
        assertThat(saved.get(0).getDisplayOrder()).isZero();
        assertThat(saved.get(1).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void replaceActivities_acceptsEmptyList_clearsAllRows() {
        // Staff edited a report and removed every activity → DTO carries [].
        List<SiteReportActivity> saved = service.replaceActivities(7L, List.of());

        verify(activityRepository).deleteByReportId(7L);
        assertThat(saved).isEmpty();
    }

    @Test
    void replaceActivities_rejectsBlankName() {
        assertThatThrownBy(() -> service.replaceActivities(7L, List.of(
                new SiteReportActivityRequest("  ", 4, null, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void replaceActivities_rejectsNegativeManpower() {
        assertThatThrownBy(() -> service.replaceActivities(7L, List.of(
                new SiteReportActivityRequest("Plastering", -1, null, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manpower");
    }

    @Test
    void replaceActivities_rejectsUnknownReportId() {
        when(siteReportRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.replaceActivities(404L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Site report not found");
    }

    @Test
    void totalManpower_sumsAllActivitiesIgnoringNulls() {
        SiteReportActivity a = new SiteReportActivity();
        a.setManpower(8);
        SiteReportActivity b = new SiteReportActivity();
        b.setManpower(4);
        SiteReportActivity c = new SiteReportActivity();
        c.setManpower(null); // partial data — defensive
        when(activityRepository.findByReportIdOrderByDisplayOrderAsc(7L))
                .thenReturn(List.of(a, b, c));

        assertThat(service.totalManpower(7L)).isEqualTo(12);
    }
}
