package com.wd.api.repository;

import com.wd.api.model.ChangeOrderPaymentSchedule;
import com.wd.api.model.enums.COPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChangeOrderPaymentScheduleRepository extends JpaRepository<ChangeOrderPaymentSchedule, Long> {

    Optional<ChangeOrderPaymentSchedule> findByChangeOrderId(Long changeOrderId);

    boolean existsByChangeOrderId(Long changeOrderId);

    /** All schedules for a project whose advance is still unpaid — used by finance dashboard. */
    @Query("SELECT s FROM ChangeOrderPaymentSchedule s " +
           "WHERE s.changeOrder.project.id = :projectId " +
           "AND s.advanceStatus = :status")
    List<ChangeOrderPaymentSchedule> findByProjectIdAndAdvanceStatus(
            @Param("projectId") Long projectId,
            @Param("status") COPaymentStatus status);

    /** Schedules whose progress tranche is triggered by a specific payment stage. */
    List<ChangeOrderPaymentSchedule> findByProgressTriggerStageId(Long stageId);
}
