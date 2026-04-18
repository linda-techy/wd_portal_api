package com.wd.api.repository;

import com.wd.api.model.SubcontractPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractPaymentRepository extends JpaRepository<SubcontractPayment, Long> {
        List<SubcontractPayment> findByWorkOrderId(Long workOrderId);
        List<SubcontractPayment> findByWorkOrderIdOrderByPaymentDateDesc(Long workOrderId);

        /** Total subcontract cost paid across all work orders. */
        @org.springframework.data.jpa.repository.Query(
                "SELECT COALESCE(SUM(sp.grossAmount), 0) FROM SubcontractPayment sp")
        java.math.BigDecimal sumTotalSubcontractCost();
}
