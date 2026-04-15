package com.wd.api.repository;

import com.wd.api.model.FinalAccount;
import com.wd.api.model.enums.FinalAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinalAccountRepository extends JpaRepository<FinalAccount, Long> {

    /** Primary lookup — one final account per project. */
    Optional<FinalAccount> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);

    /** All final accounts at a given status — used by finance team dashboards. */
    List<FinalAccount> findByStatus(FinalAccountStatus status);

    /** Projects whose DLP has ended but retention has not yet been released. */
    @Query("SELECT fa FROM FinalAccount fa " +
           "WHERE fa.retentionReleased = false " +
           "AND fa.dlpEndDate IS NOT NULL " +
           "AND fa.dlpEndDate <= CURRENT_DATE")
    List<FinalAccount> findPendingRetentionRelease();
}
