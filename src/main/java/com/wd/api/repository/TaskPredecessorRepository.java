package com.wd.api.repository;

import com.wd.api.model.scheduling.TaskPredecessor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskPredecessorRepository extends JpaRepository<TaskPredecessor, Long> {
    List<TaskPredecessor> findBySuccessorId(Long successorId);
    List<TaskPredecessor> findByPredecessorId(Long predecessorId);
    void deleteBySuccessorId(Long successorId);
    long countBySuccessorId(Long successorId);
}
