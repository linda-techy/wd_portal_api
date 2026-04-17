package com.wd.api.repository;

import com.wd.api.model.LabourPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LabourPaymentRepository extends JpaRepository<LabourPayment, Long> {
    List<LabourPayment> findByProjectId(Long projectId);

    List<LabourPayment> findByLabourId(Long labourId);

    /** Total labour cost paid across all projects (for dashboard finance KPI). */
    @Query("SELECT COALESCE(SUM(lp.amount), 0) FROM LabourPayment lp")
    BigDecimal sumTotalLabourCost();

    @Query("SELECT COALESCE(SUM(lp.amount), 0) FROM LabourPayment lp WHERE lp.wageSheet.id = :wageSheetId AND lp.labour.id = :labourId")
    BigDecimal sumPaymentsByWageSheetAndLabour(@Param("wageSheetId") Long wageSheetId, @Param("labourId") Long labourId);
}
