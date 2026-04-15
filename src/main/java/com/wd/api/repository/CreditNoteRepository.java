package com.wd.api.repository;

import com.wd.api.model.CreditNote;
import com.wd.api.model.enums.CreditNoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    List<CreditNote> findByProjectIdOrderByIssuedAtDesc(Long projectId);

    List<CreditNote> findByProjectIdAndStatus(Long projectId, CreditNoteStatus status);

    long countByProjectId(Long projectId);

    /** Sum of remaining_balance for all AVAILABLE or PARTIALLY_APPLIED credit notes. */
    @Query("SELECT COALESCE(SUM(cn.remainingBalance), 0) FROM CreditNote cn " +
           "WHERE cn.project.id = :projectId AND cn.status IN ('AVAILABLE','PARTIALLY_APPLIED')")
    BigDecimal sumAvailableCredit(@Param("projectId") Long projectId);

    List<CreditNote> findByProjectIdAndStatusInOrderByIssuedAtAsc(Long projectId, List<CreditNoteStatus> statuses);
}
