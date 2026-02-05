package com.wd.api.repository;

import com.wd.api.model.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long>, JpaSpecificationExecutor<Observation> {

    List<Observation> findByProjectIdOrderByReportedDateDesc(Long projectId);

    Page<Observation> findByProjectId(Long projectId, Pageable pageable);

    List<Observation> findByProjectIdAndStatusOrderByReportedDateDesc(Long projectId, String status);

    List<Observation> findByProjectIdAndStatusInOrderByReportedDateDesc(Long projectId, List<String> statuses);

    List<Observation> findByReportedByIdOrderByReportedDateDesc(Long reportedById);

    @Query("SELECT o FROM Observation o WHERE o.project.id = :projectId AND o.status IN ('OPEN', 'IN_PROGRESS') ORDER BY o.priority DESC, o.reportedDate DESC")
    List<Observation> findActiveByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT o FROM Observation o WHERE o.project.id = :projectId AND o.status = 'RESOLVED' ORDER BY o.resolvedDate DESC")
    List<Observation> findResolvedByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(o) FROM Observation o WHERE o.project.id = :projectId AND o.status IN ('OPEN', 'IN_PROGRESS')")
    long countActiveByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(o) FROM Observation o WHERE o.project.id = :projectId AND o.status = 'RESOLVED'")
    long countResolvedByProjectId(@Param("projectId") Long projectId);

    long countByProjectId(Long projectId);
}
