package com.wd.api.repository;

import com.wd.api.model.BoqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long>, JpaSpecificationExecutor<BoqItem> {
    List<BoqItem> findByProjectId(Long projectId);

    List<BoqItem> findByProjectIdAndDeletedAtIsNull(Long projectId);

    List<BoqItem> findByProjectIdAndWorkTypeId(Long projectId, Long workTypeId);

    List<BoqItem> findByProjectIdAndItemCodeAndDeletedAtIsNull(Long projectId, String itemCode);
    
    // CRITICAL FIX: Count active items in a category (for safe deletion)
    long countByCategory_IdAndDeletedAtIsNull(Long categoryId);
}
