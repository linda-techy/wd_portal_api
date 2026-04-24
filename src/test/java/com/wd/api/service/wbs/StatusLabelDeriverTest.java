package com.wd.api.service.wbs;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class StatusLabelDeriverTest {

    private final StatusLabelDeriver deriver = new StatusLabelDeriver();

    @Test
    void completedWithinPlannedRangeIsOnSchedule() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThat(deriver.derive(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 30), today, 100))
                .isEqualTo(StatusLabelDeriver.Label.ON_SCHEDULE);
    }

    @Test
    void pastPlannedEndAndIncompleteIsDelayed() {
        LocalDate today = LocalDate.of(2026, 5, 5);
        assertThat(deriver.derive(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 30), today, 60))
                .isEqualTo(StatusLabelDeriver.Label.DELAYED);
    }

    @Test
    void withinRangeAndProgressFarBehindElapsedIsAtRisk() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThat(deriver.derive(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 30), today, 10))
                .isEqualTo(StatusLabelDeriver.Label.AT_RISK);
    }

    @Test
    void withinRangeAndProgressMatchesElapsedIsOnTrack() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThat(deriver.derive(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 30), today, 50))
                .isEqualTo(StatusLabelDeriver.Label.ON_TRACK);
    }

    @Test
    void notYetStartedReturnsOnTrack() {
        LocalDate today = LocalDate.of(2026, 4, 22);
        assertThat(deriver.derive(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), today, 0))
                .isEqualTo(StatusLabelDeriver.Label.ON_TRACK);
    }
}
