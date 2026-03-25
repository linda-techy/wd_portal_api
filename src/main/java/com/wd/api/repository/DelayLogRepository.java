package com.wd.api.repository;

import com.wd.api.model.DelayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DelayLogRepository extends JpaRepository<DelayLog, Long>, JpaSpecificationExecutor<DelayLog> {
    List<DelayLog> findByProjectIdOrderByFromDateDesc(Long projectId);

    List<DelayLog> findByProjectIdAndDelayType(Long projectId, String delayType);

    List<DelayLog> findByProjectId(Long projectId);

    /** Count of active delays: those with no end date (still ongoing). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(d) FROM DelayLog d WHERE d.toDate IS NULL")
    long countActive();

    /** Count active delays for a specific project. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(d) FROM DelayLog d WHERE d.project.id = :projectId AND d.toDate IS NULL")
    int countActiveByProjectId(@org.springframework.data.repository.query.Param("projectId") Long projectId);
}
