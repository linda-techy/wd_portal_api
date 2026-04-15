package com.wd.api.repository;

import com.wd.api.model.LabourPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabourPaymentRepository extends JpaRepository<LabourPayment, Long> {
    List<LabourPayment> findByProjectId(Long projectId);

    List<LabourPayment> findByLabourId(Long labourId);

    /** Total labour cost paid across all projects (for dashboard finance KPI). */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(lp.amount), 0) FROM LabourPayment lp")
    java.math.BigDecimal sumTotalLabourCost();
}
