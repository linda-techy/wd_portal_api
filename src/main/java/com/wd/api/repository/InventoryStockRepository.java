package com.wd.api.repository;

import com.wd.api.model.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    List<InventoryStock> findByProjectId(Long projectId);

    Optional<InventoryStock> findByProjectIdAndMaterialId(Long projectId, Long materialId);
}
