package com.wd.api.repository;

import com.wd.api.model.scheduling.TaskPredecessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TaskPredecessorRepository extends JpaRepository<TaskPredecessor, Long> {
    List<TaskPredecessor> findBySuccessorId(Long successorId);
    List<TaskPredecessor> findByPredecessorId(Long predecessorId);
    void deleteBySuccessorId(Long successorId);
    long countBySuccessorId(Long successorId);

    /**
     * Bulk-fetch every predecessor edge whose successor is in {@code successorIds}.
     * Used by CpmService.recompute to load all edges for a project's tasks
     * in a single query instead of N findBySuccessorId calls.
     */
    @Query("SELECT p FROM TaskPredecessor p WHERE p.successorId IN :successorIds")
    List<TaskPredecessor> findBySuccessorIdIn(@Param("successorIds") Collection<Long> successorIds);
}
