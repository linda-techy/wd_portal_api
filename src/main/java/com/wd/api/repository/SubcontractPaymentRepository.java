package com.wd.api.repository;

import com.wd.api.model.SubcontractPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubcontractPaymentRepository extends JpaRepository<SubcontractPayment, Long> {

    // Find by work order
    List<SubcontractPayment> findByWorkOrderId(Long workOrderId);

    // Find by payment date range
    List<SubcontractPayment> findByPaymentDateBetween(LocalDate start, LocalDate end);

    // Find by payment mode
    List<SubcontractPayment> findByPaymentMode(SubcontractPayment.PaymentMode paymentMode);

    // Find advance payments
    List<SubcontractPayment> findByIsAdvancePaymentTrue();

    // Find by paid by user
    List<SubcontractPayment> findByPaidById(Long userId);

    // Get total paid for a work order
    @Query("SELECT COALESCE(SUM(p.grossAmount), 0) FROM SubcontractPayment p WHERE p.workOrder.id = :workOrderId")
    BigDecimal getTotalPaidAmount(@Param("workOrderId") Long workOrderId);

    // Get total TDS deducted for a work order
    @Query("SELECT COALESCE(SUM(p.tdsAmount), 0) FROM SubcontractPayment p WHERE p.workOrder.id = :workOrderId")
    BigDecimal getTotalTdsAmount(@Param("workOrderId") Long workOrderId);

    // Count payments for a work order
    long countByWorkOrderId(Long workOrderId);

    // Get payments for a specific project (via work order)
    @Query("SELECT p FROM SubcontractPayment p WHERE p.workOrder.project.id = :projectId ORDER BY p.paymentDate DESC")
    List<SubcontractPayment> findByProjectId(@Param("projectId") Long projectId);

    // Get vendor payments in date range
    @Query("SELECT p FROM SubcontractPayment p WHERE p.workOrder.vendor.id = :vendorId " +
            "AND p.paymentDate BETWEEN :startDate AND :endDate")
    List<SubcontractPayment> findVendorPayments(@Param("vendorId") Long vendorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
