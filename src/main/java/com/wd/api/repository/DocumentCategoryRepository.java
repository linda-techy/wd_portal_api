package com.wd.api.repository;

import com.wd.api.model.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {
    
    Optional<DocumentCategory> findByName(String name);

    List<DocumentCategory> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Find categories by reference type (LEAD, PROJECT, or BOTH)
     * Note: Categories with null reference_type are excluded (migration sets default to 'BOTH')
     */
    @Query(
        "SELECT c FROM DocumentCategory c WHERE " +
        "(c.referenceType = :referenceType OR c.referenceType = 'BOTH') " +
        "ORDER BY c.displayOrder ASC"
    )
    List<DocumentCategory> findByReferenceTypeOrBoth(@Param("referenceType") String referenceType);
}

