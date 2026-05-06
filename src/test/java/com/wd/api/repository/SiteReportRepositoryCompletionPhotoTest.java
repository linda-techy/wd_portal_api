package com.wd.api.repository;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.SiteReport;
import com.wd.api.model.Task;
import com.wd.api.model.enums.ReportType;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the derived-query gate predicate the FSM relies on:
 * {@code existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull}.
 * Only a geotagged COMPLETION-type SiteReport unlocks markComplete.
 */
@Transactional
class SiteReportRepositoryCompletionPhotoTest extends TestcontainersPostgresBase {

    @Autowired private SiteReportRepository repo;
    @Autowired private EntityManager em;

    private Task seedTask() {
        CustomerProject p = new CustomerProject();
        p.setName("p");
        p.setLocation("Test Location");
        em.persist(p);

        Task t = new Task();
        t.setTitle("T1");
        t.setStatus(Task.TaskStatus.IN_PROGRESS);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.now().plusDays(7));
        em.persist(t);
        return t;
    }

    private SiteReport seedReport(Task t, ReportType type, Double lat, Double lng) {
        SiteReport r = new SiteReport();
        r.setProject(t.getProject());
        r.setTitle("r");
        r.setReportType(type);
        r.setTaskId(t.getId());
        r.setLatitude(lat);
        r.setLongitude(lng);
        return repo.save(r);
    }

    @Test
    void returnsTrueWhenCompletionReportHasGeotag() {
        Task t = seedTask();
        seedReport(t, ReportType.COMPLETION, 9.9312, 76.2673);
        em.flush();
        boolean has = repo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                t.getId(), ReportType.COMPLETION);
        assertThat(has).isTrue();
    }

    @Test
    void returnsFalseWhenCompletionReportLacksGeotag() {
        Task t = seedTask();
        seedReport(t, ReportType.COMPLETION, null, null);
        em.flush();
        boolean has = repo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                t.getId(), ReportType.COMPLETION);
        assertThat(has).isFalse();
    }

    @Test
    void returnsFalseWhenOnlyDailyProgressReportPresent() {
        Task t = seedTask();
        seedReport(t, ReportType.DAILY_PROGRESS, 9.9, 76.2);
        em.flush();
        boolean has = repo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                t.getId(), ReportType.COMPLETION);
        assertThat(has).isFalse();
    }

    @Test
    void returnsFalseWhenLatPresentButLngMissing() {
        Task t = seedTask();
        seedReport(t, ReportType.COMPLETION, 9.9, null);
        em.flush();
        boolean has = repo.existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
                t.getId(), ReportType.COMPLETION);
        assertThat(has).isFalse();
    }
}
