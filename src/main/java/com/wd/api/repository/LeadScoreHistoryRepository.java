package com.wd.api.repository;

import com.wd.api.model.LeadScoreHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for LeadScoreHistory entity
 * Provides data access for lead score change history
 */
@Repository
public interface LeadScoreHistoryRepository extends JpaRepository<LeadScoreHistory, Long> {

    /**
     * Find all score history for a specific lead, ordered by most recent first
     */
    List<LeadScoreHistory> findByLeadIdOrderByScoredAtDesc(Long leadId);

    /**
     * Find score history for a lead with pagination
     */
    Page<LeadScoreHistory> findByLeadIdOrderByScoredAtDesc(Long leadId, Pageable pageable);

    /**
     * Find score history for a lead within a date range
     */
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.leadId = :leadId " +
           "AND h.scoredAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.scoredAt DESC")
    List<LeadScoreHistory> findByLeadIdAndScoredAtBetween(
            @Param("leadId") Long leadId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get the latest score history entry for a lead
     */
    @Query("SELECT h FROM LeadScoreHistory h WHERE h.leadId = :leadId " +
           "ORDER BY h.scoredAt DESC")
    List<LeadScoreHistory> findLatestByLeadId(@Param("leadId") Long leadId, Pageable pageable);

    /**
     * Count total score changes for a lead
     */
    long countByLeadId(Long leadId);
}
