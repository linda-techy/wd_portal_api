package com.wd.api.repository;

import com.wd.api.model.TaskAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAssignmentHistoryRepository extends JpaRepository<TaskAssignmentHistory, Long> {

    /**
     * Get all assignment history for a task, ordered by most recent first
     */
    List<TaskAssignmentHistory> findByTaskIdOrderByAssignedAtDesc(Long taskId);
}
