package com.wd.api.repository;

import com.wd.api.model.BoqItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long>, JpaSpecificationExecutor<BoqItem> {
    List<BoqItem> findByProjectId(Long projectId);

    List<BoqItem> findByProjectIdAndDeletedAtIsNull(Long projectId);

    List<BoqItem> findByProjectIdAndWorkTypeId(Long projectId, Long workTypeId);

    List<BoqItem> findByProjectIdAndItemCodeAndDeletedAtIsNull(Long projectId, String itemCode);
    
    // CRITICAL FIX: Count active items in a category (for safe deletion)
    long countByCategory_IdAndDeletedAtIsNull(Long categoryId);

    // Pessimistic write lock for concurrent execution/billing updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BoqItem b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<BoqItem> findByIdWithLock(@Param("id") Long id);
}
