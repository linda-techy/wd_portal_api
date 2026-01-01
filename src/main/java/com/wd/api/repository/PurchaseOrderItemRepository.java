package com.wd.api.repository;

import com.wd.api.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @Query("SELECT SUM(poi.quantity) FROM PurchaseOrderItem poi " +
            "WHERE poi.purchaseOrder.project.id = :projectId " +
            "AND poi.material.id = :materialId " +
            "AND poi.purchaseOrder.status <> 'CANCELLED'")
    Optional<BigDecimal> sumQuantityByProjectAndMaterial(@Param("projectId") Long projectId,
            @Param("materialId") Long materialId);
}
