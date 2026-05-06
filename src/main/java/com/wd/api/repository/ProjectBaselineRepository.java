package com.wd.api.repository;

import com.wd.api.model.scheduling.ProjectBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectBaselineRepository extends JpaRepository<ProjectBaseline, Long> {

    Optional<ProjectBaseline> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}
