package com.wd.api.service.wbs;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TimelineBucketingServiceTest {

    private final TimelineBucketingService service = new TimelineBucketingService();

    private static class T implements TimelineBucketingService.TaskLike {
        final LocalDate s, e; final String st; final int p;
        T(LocalDate s, LocalDate e, String st, int p) { this.s=s; this.e=e; this.st=st; this.p=p; }
        @Override public LocalDate startDate() { return s; }
        @Override public LocalDate endDate() { return e; }
        @Override public String status() { return st; }
        @Override public int progressPercent() { return p; }
    }

    @Test
    void taskOverlappingThisWeekIsWeek() {
        LocalDate today = LocalDate.of(2026, 4, 22); // Wednesday
        T task = new T(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 24), "IN_PROGRESS", 50);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.WEEK);
    }

    @Test
    void taskStartingTwoWeeksOutIsUpcoming() {
        LocalDate today = LocalDate.of(2026, 4, 22);
        T task = new T(LocalDate.of(2026, 5, 6), LocalDate.of(2026, 5, 10), "PENDING", 0);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.UPCOMING);
    }

    @Test
    void completedStatusBeatsDateClassification() {
        LocalDate today = LocalDate.of(2026, 4, 22);
        T task = new T(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), "COMPLETED", 100);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.COMPLETED);
    }

    @Test
    void taskEndingSundayIsStillThisWeek() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        T task = new T(LocalDate.of(2026, 4, 22), LocalDate.of(2026, 4, 26), "IN_PROGRESS", 30);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.WEEK);
    }

    @Test
    void taskStartingNextMondayIsUpcoming() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        T task = new T(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 4, 30), "PENDING", 0);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.UPCOMING);
    }

    @Test
    void taskAt100PercentClassifiesAsCompleted() {
        LocalDate today = LocalDate.of(2026, 4, 22);
        T task = new T(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 24), "PENDING", 100);
        assertThat(service.classify(task, today)).isEqualTo(TimelineBucketingService.Bucket.COMPLETED);
    }

    @Test
    void weekBoundsStartOnMonday() {
        LocalDate today = LocalDate.of(2026, 4, 22);
        TimelineBucketingService.WeekBounds bounds = service.weekBounds(today);
        assertThat(bounds.start().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(bounds.start()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(bounds.end()).isEqualTo(LocalDate.of(2026, 4, 26));
    }
}
