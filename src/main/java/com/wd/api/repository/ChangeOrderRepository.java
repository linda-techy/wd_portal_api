package com.wd.api.repository;

import com.wd.api.model.ChangeOrder;
import com.wd.api.model.enums.ChangeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChangeOrderRepository extends JpaRepository<ChangeOrder, Long> {

    List<ChangeOrder> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long projectId);

    List<ChangeOrder> findByProjectIdAndStatusAndDeletedAtIsNull(Long projectId, ChangeOrderStatus status);

    List<ChangeOrder> findByBoqDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long boqDocumentId);

    Optional<ChangeOrder> findByProjectIdAndReferenceNumber(Long projectId, String referenceNumber);

    /** Count COs for a project to generate the next reference number. */
    long countByProjectIdAndDeletedAtIsNull(Long projectId);

    @Query("SELECT co FROM ChangeOrder co LEFT JOIN FETCH co.lineItems " +
           "WHERE co.id = :id AND co.deletedAt IS NULL")
    Optional<ChangeOrder> findByIdWithLineItems(@Param("id") Long id);

    @Query("SELECT co FROM ChangeOrder co LEFT JOIN FETCH co.lineItems LEFT JOIN FETCH co.milestones " +
           "WHERE co.id = :id AND co.deletedAt IS NULL")
    Optional<ChangeOrder> findByIdWithDetails(@Param("id") Long id);
}
