package com.wd.api.repository;

import com.wd.api.model.BoqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoqCategoryRepository extends JpaRepository<BoqCategory, Long> {

    List<BoqCategory> findByProjectIdAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(Long projectId);

    List<BoqCategory> findByProjectIdAndParentIdAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(
            Long projectId, Long parentId);

    @Query("SELECT c FROM BoqCategory c WHERE c.project.id = :projectId AND c.parent IS NULL AND c.deletedAt IS NULL ORDER BY c.displayOrder, c.name")
    List<BoqCategory> findTopLevelCategoriesByProject(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(b) FROM BoqItem b WHERE b.category.id = :categoryId AND b.deletedAt IS NULL")
    int countItemsByCategory(@Param("categoryId") Long categoryId);
    
    // CRITICAL FIX: Count subcategories for safe deletion
    long countByParentIdAndIsActiveTrue(Long parentId);
}
