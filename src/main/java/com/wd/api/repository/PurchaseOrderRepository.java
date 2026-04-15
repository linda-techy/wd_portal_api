package com.wd.api.repository;

import com.wd.api.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {
    List<PurchaseOrder> findByProjectId(Long projectId);

    List<PurchaseOrder> findByVendorId(Long vendorId);

    /** Total procurement cost across all non-cancelled purchase orders. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(po.totalAmount), 0) FROM PurchaseOrder po " +
            "WHERE po.status != com.wd.api.model.enums.PurchaseOrderStatus.CANCELLED")
    java.math.BigDecimal sumTotalProcurementCost();
}
