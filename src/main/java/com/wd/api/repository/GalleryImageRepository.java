package com.wd.api.repository;

import com.wd.api.model.GalleryImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long>, JpaSpecificationExecutor<GalleryImage> {

    List<GalleryImage> findByProjectIdOrderByTakenDateDesc(Long projectId);

    Page<GalleryImage> findByProjectId(Long projectId, Pageable pageable);

    List<GalleryImage> findByProjectIdAndTakenDateBetweenOrderByTakenDateDesc(
            Long projectId, LocalDate startDate, LocalDate endDate);

    List<GalleryImage> findBySiteReportId(Long siteReportId);

    @Query("SELECT g FROM GalleryImage g WHERE g.project.id = :projectId ORDER BY g.takenDate DESC")
    List<GalleryImage> findAllByProjectOrderByDate(@Param("projectId") Long projectId);

    @Query("SELECT DISTINCT g.takenDate FROM GalleryImage g WHERE g.project.id = :projectId ORDER BY g.takenDate DESC")
    List<LocalDate> findDistinctTakenDatesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT g FROM GalleryImage g WHERE g.project.id = :projectId AND g.takenDate = :date ORDER BY g.uploadedAt DESC")
    List<GalleryImage> findByProjectIdAndTakenDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    long countByProjectId(Long projectId);
}
