package com.wd.api.repository;

import com.wd.api.model.TaskQualityGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskQualityGateRepository extends JpaRepository<TaskQualityGate, Long> {

    /** Every gate for a task, ordered by the natural ITP sequence. */
    @Query("""
        SELECT g FROM TaskQualityGate g
        LEFT JOIN FETCH g.signedBy
        WHERE g.task.id = :taskId
        ORDER BY CASE g.gateType
            WHEN com.wd.api.model.TaskQualityGate.GateType.PRELIMINARY THEN 1
            WHEN com.wd.api.model.TaskQualityGate.GateType.IN_PROGRESS THEN 2
            WHEN com.wd.api.model.TaskQualityGate.GateType.FINAL       THEN 3
        END
        """)
    List<TaskQualityGate> findByTaskId(@Param("taskId") Long taskId);

    Optional<TaskQualityGate> findByTaskIdAndGateType(Long taskId, TaskQualityGate.GateType gateType);

    long countByTaskIdAndStatus(Long taskId, TaskQualityGate.Status status);
}
