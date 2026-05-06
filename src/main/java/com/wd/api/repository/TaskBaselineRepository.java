package com.wd.api.repository;

import com.wd.api.model.scheduling.TaskBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskBaselineRepository extends JpaRepository<TaskBaseline, Long> {

    List<TaskBaseline> findByBaselineId(Long baselineId);

    Optional<TaskBaseline> findByBaselineIdAndTaskId(Long baselineId, Long taskId);
}
