package com.wd.api.repository;

import com.wd.api.model.SiteReportActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SiteReportActivityRepository extends JpaRepository<SiteReportActivity, Long> {

    List<SiteReportActivity> findByReportIdOrderByDisplayOrderAsc(Long reportId);

    /**
     * Bulk delete used by the replace-all save path. Modifying query (not
     * the derived {@code deleteByReportId}) so we issue a single DELETE
     * instead of N DELETEs.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SiteReportActivity a WHERE a.report.id = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);
}
