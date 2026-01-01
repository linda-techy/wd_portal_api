package com.wd.api.repository;

import com.wd.api.model.SubcontractMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubcontractMeasurementRepository extends JpaRepository<SubcontractMeasurement, Long> {

    // Find by work order
    List<SubcontractMeasurement> findByWorkOrderId(Long workOrderId);

    // Find by status
    List<SubcontractMeasurement> findByStatus(SubcontractMeasurement.MeasurementStatus status);

    // Find pending measurements for a work order
    List<SubcontractMeasurement> findByWorkOrderIdAndStatus(Long workOrderId,
            SubcontractMeasurement.MeasurementStatus status);

    // Find measurements by date range
    List<SubcontractMeasurement> findByMeasurementDateBetween(LocalDate start, LocalDate end);

    // Find by measured by user
    List<SubcontractMeasurement> findByMeasuredById(Long userId);

    // Get total approved amount for a work order
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM SubcontractMeasurement m " +
            "WHERE m.workOrder.id = :workOrderId AND m.status = 'APPROVED'")
    BigDecimal getTotalApprovedAmount(@Param("workOrderId") Long workOrderId);

    // Get total pending amount for a work order
    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM SubcontractMeasurement m " +
            "WHERE m.workOrder.id = :workOrderId AND m.status = 'PENDING'")
    BigDecimal getTotalPendingAmount(@Param("workOrderId") Long workOrderId);

    // Count pending measurements
    long countByStatus(SubcontractMeasurement.MeasurementStatus status);

    // Get next bill number for work order
    @Query("SELECT COUNT(m) + 1 FROM SubcontractMeasurement m WHERE m.workOrder.id = :workOrderId")
    int getNextBillNumber(@Param("workOrderId") Long workOrderId);
}
