package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.PortalUser;
import com.wd.api.model.SiteReport;
import com.wd.api.model.Task;
import com.wd.api.model.enums.ReportType;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests that the existing {@code createReport(SiteReport, List, PortalUser)}
 * contract round-trips the new {@link SiteReport#taskId} field and that the
 * entity-level default ({@code DAILY_PROGRESS}) takes effect when reportType
 * is not set. No service-body changes are needed for S3 PR2 — the new column
 * is auto-mapped via the entity edit in Task 7.
 */
@Transactional
class SiteReportServiceTaskLinkTest extends TestcontainersPostgresBase {

    @Autowired private SiteReportService service;
    @Autowired private EntityManager em;

    private PortalUser seedUser() {
        PortalUser u = new PortalUser();
        u.setEmail("se@example.com");
        u.setFirstName("Site");
        u.setLastName("Engineer");
        u.setEnabled(true);
        u.setPassword("not-a-real-hash");
        em.persist(u);
        return u;
    }

    private Task seedTaskForProject(CustomerProject p) {
        Task t = new Task();
        t.setTitle("Beam casting");
        t.setStatus(Task.TaskStatus.IN_PROGRESS);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.now().plusDays(7));
        em.persist(t);
        return t;
    }

    @Test
    void createReport_withTaskIdAndCompletionType_persistsBothFields() {
        PortalUser u = seedUser();
        CustomerProject p = new CustomerProject();
        p.setName("Test Project");
        p.setLocation("Test Location");
        em.persist(p);
        Task t = seedTaskForProject(p);

        SiteReport report = new SiteReport();
        report.setProject(p);
        report.setTitle("Beam complete");
        report.setReportType(ReportType.COMPLETION);
        report.setTaskId(t.getId());
        report.setLatitude(9.9312);
        report.setLongitude(76.2673);

        SiteReport saved = service.createReport(report, List.<MultipartFile>of(), u);

        assertThat(saved.getTaskId()).isEqualTo(t.getId());
        assertThat(saved.getReportType()).isEqualTo(ReportType.COMPLETION);
    }

    @Test
    void createReport_withoutReportType_defaultsToDailyProgress() {
        PortalUser u = seedUser();
        CustomerProject p = new CustomerProject();
        p.setName("Test Project 2");
        p.setLocation("Test Location 2");
        em.persist(p);

        SiteReport report = new SiteReport();
        report.setProject(p);
        report.setTitle("Daily diary");
        // reportType deliberately not set; entity default is DAILY_PROGRESS.
        SiteReport saved = service.createReport(report, List.<MultipartFile>of(), u);

        assertThat(saved.getReportType()).isEqualTo(ReportType.DAILY_PROGRESS);
        assertThat(saved.getTaskId()).isNull();
    }
}
