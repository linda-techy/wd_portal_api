package com.wd.api.repository;

import com.wd.api.model.ProjectProgressLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectProgressLogRepository extends JpaRepository<ProjectProgressLog, Long> {

    /**
     * Find all logs for a project, ordered by date descending
     */
    List<ProjectProgressLog> findByProjectIdOrderByChangedAtDesc(Long projectId);

    /**
     * Find logs for a project with pagination
     */
    Page<ProjectProgressLog> findByProjectId(Long projectId, Pageable pageable);

    /**
     * Find logs by change type
     */
    List<ProjectProgressLog> findByProjectIdAndChangeType(Long projectId, String changeType);

    /**
     * Find logs within date range
     */
    @Query("SELECT pl FROM ProjectProgressLog pl WHERE pl.project.id = :projectId " +
           "AND pl.changedAt BETWEEN :startDate AND :endDate ORDER BY pl.changedAt DESC")
    List<ProjectProgressLog> findByProjectIdAndDateRange(Long projectId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find recent logs (last N)
     */
    List<ProjectProgressLog> findTop10ByProjectIdOrderByChangedAtDesc(Long projectId);

    /**
     * Count logs for a project
     */
    long countByProjectId(Long projectId);

    /**
     * Find logs by user who made the change
     */
    List<ProjectProgressLog> findByChangedByIdOrderByChangedAtDesc(Long userId);
}

