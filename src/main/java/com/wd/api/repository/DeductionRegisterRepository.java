package com.wd.api.repository;

import com.wd.api.model.DeductionRegister;
import com.wd.api.model.enums.DeductionDecision;
import com.wd.api.model.enums.EscalationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DeductionRegisterRepository extends JpaRepository<DeductionRegister, Long> {

    List<DeductionRegister> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<DeductionRegister> findByProjectIdAndDecision(Long projectId, DeductionDecision decision);

    List<DeductionRegister> findByProjectIdAndEscalationStatus(Long projectId, EscalationStatus escalationStatus);

    /** All unsettled deductions for a project — used when preparing a final account. */
    List<DeductionRegister> findByProjectIdAndSettledInFinalAccountFalse(Long projectId);

    /** Sum of accepted amounts for a project — used to populate final_account.total_accepted_deductions. */
    @Query("SELECT COALESCE(SUM(d.acceptedAmount), 0) FROM DeductionRegister d " +
           "WHERE d.project.id = :projectId " +
           "AND d.decision IN ('ACCEPTABLE','PARTIALLY_ACCEPTABLE')")
    BigDecimal sumAcceptedAmountsByProjectId(@Param("projectId") Long projectId);

    List<DeductionRegister> findByChangeOrderId(Long changeOrderId);
}
