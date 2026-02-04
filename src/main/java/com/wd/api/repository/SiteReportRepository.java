package com.wd.api.repository;

import com.wd.api.model.SiteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface SiteReportRepository extends JpaRepository<SiteReport, Long>, JpaSpecificationExecutor<SiteReport> {
    List<SiteReport> findByProjectIdOrderByReportDateDesc(Long projectId);

    List<SiteReport> findBySubmittedByIdOrderByReportDateDesc(Long userId);

    /**
     * Find site reports by multiple project IDs with pagination.
     * Used by customer portal to fetch reports for all customer's projects.
     */
    Page<SiteReport> findByProjectIdIn(List<Long> projectIds, Pageable pageable);
}
