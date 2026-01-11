package com.wd.api.repository;

import com.wd.api.model.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    List<ProjectMilestone> findByProjectId(Long projectId);

    List<ProjectMilestone> findByProjectIdAndStatus(Long projectId, String status);
}
