package com.wd.api.repository;

import com.wd.api.model.DpcDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DpcDocumentRepository extends JpaRepository<DpcDocument, Long> {

    List<DpcDocument> findByProjectIdOrderByRevisionNumberDesc(Long projectId);

    /** Latest revision for a project (highest revision_number first). */
    Optional<DpcDocument> findFirstByProjectIdOrderByRevisionNumberDesc(Long projectId);

    @Query("SELECT MAX(d.revisionNumber) FROM DpcDocument d WHERE d.project.id = :projectId")
    Optional<Integer> findMaxRevisionByProjectId(@Param("projectId") Long projectId);
}
