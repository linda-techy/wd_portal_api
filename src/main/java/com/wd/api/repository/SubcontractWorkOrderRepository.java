package com.wd.api.repository;

import com.wd.api.model.SubcontractWorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcontractWorkOrderRepository extends JpaRepository<SubcontractWorkOrder, Long> {

    // Find by project
    List<SubcontractWorkOrder> findByProjectId(Long projectId);

    // Find by vendor
    List<SubcontractWorkOrder> findByVendorId(Long vendorId);

    // Find by status
    List<SubcontractWorkOrder> findByStatus(SubcontractWorkOrder.WorkOrderStatus status);

    // Find active work orders (ISSUED or IN_PROGRESS)
    @Query("SELECT wo FROM SubcontractWorkOrder wo WHERE wo.status IN ('ISSUED', 'IN_PROGRESS')")
    List<SubcontractWorkOrder> findActiveWorkOrders();

    // Find by work order number
    Optional<SubcontractWorkOrder> findByWorkOrderNumber(String workOrderNumber);

    // Find by project and vendor
    List<SubcontractWorkOrder> findByProjectIdAndVendorId(Long projectId, Long vendorId);

    // Find BOQ-linked work orders
    List<SubcontractWorkOrder> findByBoqItemId(Long boqItemId);

    // Count active work orders for a project
    @Query("SELECT COUNT(wo) FROM SubcontractWorkOrder wo WHERE wo.project.id = :projectId AND wo.status IN ('ISSUED', 'IN_PROGRESS')")
    long countActiveWorkOrdersByProject(@Param("projectId") Long projectId);

    // Get work orders with pending payments
    @Query("SELECT DISTINCT wo FROM SubcontractWorkOrder wo " +
            "LEFT JOIN SubcontractMeasurement m ON m.workOrder.id = wo.id " +
            "WHERE m.status = 'APPROVED' " +
            "OR wo.measurementBasis = 'LUMPSUM'")
    List<SubcontractWorkOrder> findWorkOrdersWithPendingPayments();
}
