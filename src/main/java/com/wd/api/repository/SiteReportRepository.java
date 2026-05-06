package com.wd.api.repository;

import com.wd.api.model.SiteReport;
import com.wd.api.model.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteReportRepository extends JpaRepository<SiteReport, Long>, JpaSpecificationExecutor<SiteReport> {
    List<SiteReport> findByProjectIdOrderByReportDateDesc(Long projectId);

    List<SiteReport> findBySubmittedByIdOrderByReportDateDesc(Long userId);

    /**
     * Find site reports by multiple project IDs with pagination.
     * Used by customer portal to fetch reports for all customer's projects.
     */
    Page<SiteReport> findByProjectIdIn(List<Long> projectIds, Pageable pageable);

    /** Count all site reports submitted since a given timestamp (for dashboard KPI). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(sr) FROM SiteReport sr WHERE sr.reportDate >= :fromDate")
    long countSince(@org.springframework.data.repository.query.Param("fromDate") java.time.LocalDateTime fromDate);

    /**
     * S3 PR2 — used by TaskCompletionService.markComplete to enforce that
     * the task has at least one geotagged COMPLETION-type SiteReport before
     * allowing IN_PROGRESS → (PENDING_PM_APPROVAL | COMPLETED).
     */
    boolean existsByTaskIdAndReportTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(
            Long taskId, ReportType reportType);

    /**
     * S3 PR2 — most-recent COMPLETION SiteReport for a task. Used by the
     * PM Approval Inbox to surface the photo thumbnail.
     */
    Optional<SiteReport> findFirstByTaskIdAndReportTypeOrderByReportDateDesc(
            Long taskId, ReportType reportType);
}
