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

    @Query("SELECT b FROM BoqItem b LEFT JOIN FETCH b.workType LEFT JOIN FETCH b.category LEFT JOIN FETCH b.material WHERE b.project.id = :projectId AND b.deletedAt IS NULL")
    List<BoqItem> findByProjectIdWithAssociations(@Param("projectId") Long projectId);

    List<BoqItem> findByProjectIdAndWorkTypeId(Long projectId, Long workTypeId);

    List<BoqItem> findByProjectIdAndItemCodeAndDeletedAtIsNull(Long projectId, String itemCode);
    
    // CRITICAL FIX: Count active items in a category (for safe deletion)
    long countByCategory_IdAndDeletedAtIsNull(Long categoryId);

    // Pessimistic write lock for concurrent execution/billing updates
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BoqItem b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<BoqItem> findByIdWithLock(@Param("id") Long id);

    // ---- Aggregate queries for financial summary (avoids loading full entities into memory) ----

    /**
     * Returns a single Object[] row:
     * [0] total_items (Long), [1] active_items (Long),
     * [2] total_planned_cost, [3] total_executed_cost,
     * [4] total_billed_cost, [5] total_cost_to_complete
     */
    @Query(value = """
        SELECT COUNT(*),
               SUM(CASE WHEN is_active = true THEN 1 ELSE 0 END),
               COALESCE(SUM(total_amount), 0),
               COALESCE(SUM(executed_quantity * unit_rate), 0),
               COALESCE(SUM(billed_quantity * unit_rate), 0),
               COALESCE(SUM(GREATEST(0, (quantity - executed_quantity) * unit_rate)), 0)
        FROM boq_items
        WHERE project_id = :projectId AND deleted_at IS NULL
        """, nativeQuery = true)
    List<Object[]> getFinancialTotals(@Param("projectId") Long projectId);

    /**
     * Returns rows of Object[]:
     * [0] category_id (Long), [1] category_name (String), [2] item_count (Long),
     * [3] planned_cost, [4] executed_cost, [5] billed_cost, [6] cost_to_complete
     */
    @Query(value = """
        SELECT bc.id, bc.name, COUNT(bi.id),
               COALESCE(SUM(bi.total_amount), 0),
               COALESCE(SUM(bi.executed_quantity * bi.unit_rate), 0),
               COALESCE(SUM(bi.billed_quantity * bi.unit_rate), 0),
               COALESCE(SUM(GREATEST(0, (bi.quantity - bi.executed_quantity) * bi.unit_rate)), 0)
        FROM boq_items bi
        JOIN boq_categories bc ON bi.category_id = bc.id
        WHERE bi.project_id = :projectId AND bi.deleted_at IS NULL
        GROUP BY bc.id, bc.name
        """, nativeQuery = true)
    List<Object[]> getFinancialCategoryBreakdown(@Param("projectId") Long projectId);

    /**
     * Returns rows of Object[]:
     * [0] work_type_id (Long), [1] work_type_name (String), [2] item_count (Long),
     * [3] planned_cost, [4] executed_cost, [5] billed_cost, [6] cost_to_complete
     */
    @Query(value = """
        SELECT wt.id, wt.name, COUNT(bi.id),
               COALESCE(SUM(bi.total_amount), 0),
               COALESCE(SUM(bi.executed_quantity * bi.unit_rate), 0),
               COALESCE(SUM(bi.billed_quantity * bi.unit_rate), 0),
               COALESCE(SUM(GREATEST(0, (bi.quantity - bi.executed_quantity) * bi.unit_rate)), 0)
        FROM boq_items bi
        JOIN boq_work_types wt ON bi.work_type_id = wt.id
        WHERE bi.project_id = :projectId AND bi.deleted_at IS NULL
        GROUP BY wt.id, wt.name
        """, nativeQuery = true)
    List<Object[]> getFinancialWorkTypeBreakdown(@Param("projectId") Long projectId);
}
