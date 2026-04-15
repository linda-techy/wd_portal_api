package com.wd.api.repository;

import com.wd.api.model.ChangeOrderApprovalHistory;
import com.wd.api.model.enums.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChangeOrderApprovalHistoryRepository extends JpaRepository<ChangeOrderApprovalHistory, Long> {

    /** Full audit trail for a CO, most recent first. */
    List<ChangeOrderApprovalHistory> findByChangeOrderIdOrderByActionAtDesc(Long changeOrderId);

    /** Most recent action for a CO. */
    Optional<ChangeOrderApprovalHistory> findFirstByChangeOrderIdOrderByActionAtDesc(Long changeOrderId);

    /** Check whether any APPROVED action exists for this CO (used to gate payment schedule creation). */
    boolean existsByChangeOrderIdAndAction(Long changeOrderId, ApprovalAction action);

    /** Last approval action at a specific level. */
    @Query("SELECT h FROM ChangeOrderApprovalHistory h " +
           "WHERE h.changeOrder.id = :coId AND h.level = :level " +
           "ORDER BY h.actionAt DESC")
    List<ChangeOrderApprovalHistory> findByCoIdAndLevel(
            @Param("coId") Long coId,
            @Param("level") com.wd.api.model.enums.ApprovalLevel level);
}
