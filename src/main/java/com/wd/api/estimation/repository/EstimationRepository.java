package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.Estimation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstimationRepository extends JpaRepository<Estimation, UUID> {

    List<Estimation> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    Optional<Estimation> findByPublicViewToken(UUID publicViewToken);

    /** L — clears is_current for every (non-deleted) estimation on a lead. */
    @Modifying
    @Query("UPDATE Estimation e SET e.isCurrent = false WHERE e.leadId = :leadId AND e.isCurrent = true")
    int clearCurrentForLead(@Param("leadId") Long leadId);

    /** L — sets is_current for one estimation. Caller must clear siblings first. */
    @Modifying
    @Query("UPDATE Estimation e SET e.isCurrent = true WHERE e.id = :id")
    int markCurrent(@Param("id") UUID id);

    /** L — auto-expire: flips SENT estimations whose validUntil has passed. */
    @Modifying
    @Query("UPDATE Estimation e SET e.status = com.wd.api.estimation.domain.enums.EstimationStatus.EXPIRED " +
           "WHERE e.status = com.wd.api.estimation.domain.enums.EstimationStatus.SENT " +
           "AND e.validUntil < :today")
    int markExpiredWhereSentAndOverdue(@Param("today") LocalDate today);

    /** L — used to pick a replacement when the current estimation is deleted. */
    @Query("SELECT e FROM Estimation e WHERE e.leadId = :leadId ORDER BY e.createdAt DESC")
    List<Estimation> findActiveByLeadOrderByCreatedAtDesc(@Param("leadId") Long leadId);

    /** L — does the lead have any ACCEPTED estimation? Used by the lead-status guard. */
    @Query("SELECT COUNT(e) > 0 FROM Estimation e WHERE e.leadId = :leadId " +
           "AND e.status = com.wd.api.estimation.domain.enums.EstimationStatus.ACCEPTED")
    boolean existsAcceptedForLead(@Param("leadId") Long leadId);
}
