package com.wd.api.repository;

import com.wd.api.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<StockAdjustment> findByProjectIdAndMaterialId(Long projectId, Long materialId);
}
