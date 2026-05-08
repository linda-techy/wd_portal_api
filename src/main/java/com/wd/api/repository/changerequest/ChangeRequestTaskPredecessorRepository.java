package com.wd.api.repository.changerequest;

import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChangeRequestTaskPredecessorRepository
        extends JpaRepository<ChangeRequestTaskPredecessor, Long> {

    /**
     * Every predecessor edge whose endpoints belong to the given CR.
     * Both endpoints reference change_request_tasks.id; the join filters on
     * the parent change_request_id of either side (they must match — enforced
     * at write time by ChangeRequestTaskService.addPredecessor).
     */
    @Query("""
        SELECT p FROM ChangeRequestTaskPredecessor p
        WHERE p.successorCrTaskId IN (
            SELECT t.id FROM ChangeRequestTask t WHERE t.changeRequest.id = :crId)
        """)
    List<ChangeRequestTaskPredecessor> findByChangeRequestId(@Param("crId") Long crId);

    /**
     * Direct predecessors of a single CR task, used by the in-CR cycle check.
     */
    List<ChangeRequestTaskPredecessor> findBySuccessorCrTaskId(Long successorCrTaskId);
}
