package com.wd.api.repository;

import com.wd.api.model.SiteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SiteReportRepository extends JpaRepository<SiteReport, Long> {
    List<SiteReport> findByProjectIdOrderByReportDateDesc(Long projectId);

    List<SiteReport> findBySubmittedByIdOrderByReportDateDesc(Long userId);
}
