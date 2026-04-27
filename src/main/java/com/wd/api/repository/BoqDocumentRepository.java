package com.wd.api.repository;

import com.wd.api.model.BoqDocument;
import com.wd.api.model.enums.BoqDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoqDocumentRepository extends JpaRepository<BoqDocument, Long> {

    List<BoqDocument> findByProjectIdOrderByRevisionNumberDesc(Long projectId);

    Optional<BoqDocument> findByProjectIdAndStatus(Long projectId, BoqDocumentStatus status);

    @Query("SELECT d FROM BoqDocument d WHERE d.project.id = :projectId AND d.status = 'APPROVED' AND d.deletedAt IS NULL")
    Optional<BoqDocument> findApprovedByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT d FROM BoqDocument d WHERE d.project.id = :projectId AND d.deletedAt IS NULL ORDER BY d.revisionNumber DESC")
    List<BoqDocument> findActiveByProjectId(@Param("projectId") Long projectId);

    boolean existsByProjectIdAndStatus(Long projectId, BoqDocumentStatus status);

    /**
     * Most recently approved BoQ for a project, used by DPC creation to anchor
     * the new document onto a single frozen BoQ snapshot.
     */
    Optional<BoqDocument> findFirstByProject_IdAndStatusOrderByApprovedAtDesc(Long projectId, BoqDocumentStatus status);
}
