package com.wd.api.repository;

import com.wd.api.model.ChangeRequestApprovalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CR transition audit rows. Append-only at app layer:
 * service code is the only call site of {@link #save}; no update/delete
 * helpers are exposed beyond what {@link JpaRepository} provides
 * (and those must NOT be invoked by service code).
 */
@Repository
public interface ChangeRequestApprovalHistoryRepository
        extends JpaRepository<ChangeRequestApprovalHistory, Long> {

    List<ChangeRequestApprovalHistory> findByChangeRequestIdOrderByActionAtDesc(Long changeRequestId);

    Page<ChangeRequestApprovalHistory> findByChangeRequestIdOrderByActionAtDesc(
            Long changeRequestId, Pageable pageable);
}
