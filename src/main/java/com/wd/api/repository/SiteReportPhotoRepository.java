package com.wd.api.repository;

import com.wd.api.model.SiteReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteReportPhotoRepository extends JpaRepository<SiteReportPhoto, Long> {

    /**
     * Highest {@code display_order} on the report. Used by the
     * "add more photos" path to assign the next index without loading
     * the entire photo collection into memory just to compute MAX
     * (the previous implementation was O(N) on every call).
     * Returns {@code null} when no photos exist yet.
     */
    @Query("SELECT MAX(p.displayOrder) FROM SiteReportPhoto p WHERE p.siteReport.id = :reportId")
    Integer findMaxDisplayOrderByReportId(@Param("reportId") Long reportId);
}
