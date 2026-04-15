package com.wd.api.repository;

import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentStageRepository extends JpaRepository<PaymentStage, Long> {

    List<PaymentStage> findByBoqDocumentIdOrderByStageNumberAsc(Long boqDocumentId);

    List<PaymentStage> findByProjectIdOrderByStageNumberAsc(Long projectId);

    List<PaymentStage> findByProjectIdAndStatusOrderByStageNumberAsc(Long projectId, PaymentStageStatus status);

    /** Sum of stage_amount_incl_gst for stages not yet paid on this project. */
    @Query("SELECT COALESCE(SUM(s.stageAmountInclGst), 0) FROM PaymentStage s " +
           "WHERE s.project.id = :projectId AND s.status NOT IN ('PAID','VOID')")
    BigDecimal sumRemainingStageAmounts(@Param("projectId") Long projectId);

    /** Sum of net_payable_amount for UPCOMING and DUE stages (credit can be applied here). */
    @Query("SELECT COALESCE(SUM(s.netPayableAmount), 0) FROM PaymentStage s " +
           "WHERE s.project.id = :projectId AND s.status IN ('UPCOMING','DUE')")
    BigDecimal sumApplicableStageAmounts(@Param("projectId") Long projectId);

    /** Next stage eligible for credit application (lowest stage_number that is UPCOMING or DUE). */
    @Query("SELECT s FROM PaymentStage s WHERE s.project.id = :projectId " +
           "AND s.status IN ('UPCOMING','DUE') AND s.invoice IS NULL " +
           "ORDER BY s.stageNumber ASC")
    List<PaymentStage> findEligibleForCreditApplication(@Param("projectId") Long projectId);
}
