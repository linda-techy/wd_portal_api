package com.wd.api.repository;

import com.wd.api.model.SiteVisit;
import com.wd.api.model.enums.VisitStatus;
import com.wd.api.model.enums.VisitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long>, JpaSpecificationExecutor<SiteVisit> {

    /**
     * Find all visits for a project ordered by date
     */
    List<SiteVisit> findByProjectIdOrderByVisitDateDesc(Long projectId);

    /**
     * Find visits by status
     */
    List<SiteVisit> findByVisitStatus(VisitStatus status);

    /**
     * Find active visits (checked in but not out) for a project
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.project.id = :projectId AND sv.visitStatus = 'CHECKED_IN'")
    List<SiteVisit> findActiveVisitsByProject(@Param("projectId") Long projectId);

    /**
     * Find all currently active visits across all projects
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.visitStatus = 'CHECKED_IN' ORDER BY sv.checkInTime DESC")
    List<SiteVisit> findAllActiveVisits();

    /**
     * Find active visit for a specific user
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.visitedBy.id = :userId AND sv.visitStatus = 'CHECKED_IN'")
    Optional<SiteVisit> findActiveVisitByUser(@Param("userId") Long userId);

    /**
     * Find visits by user within date range
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.visitedBy.id = :userId " +
            "AND sv.visitDate >= :startDate AND sv.visitDate <= :endDate " +
            "ORDER BY sv.visitDate DESC")
    List<SiteVisit> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find visits by project and date range
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.project.id = :projectId " +
            "AND sv.visitDate >= :startDate AND sv.visitDate <= :endDate " +
            "ORDER BY sv.visitDate DESC")
    List<SiteVisit> findByProjectAndDateRange(
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count visits by type for a project
     */
    @Query("SELECT COUNT(sv) FROM SiteVisit sv WHERE sv.project.id = :projectId AND sv.visitType = :visitType")
    long countByProjectAndType(@Param("projectId") Long projectId, @Param("visitType") VisitType visitType);

    /**
     * Find today's visits for a project
     */
    @Query("SELECT sv FROM SiteVisit sv WHERE sv.project.id = :projectId " +
            "AND CAST(sv.visitDate AS date) = CURRENT_DATE ORDER BY sv.checkInTime DESC")
    List<SiteVisit> findTodaysVisitsByProject(@Param("projectId") Long projectId);
}
