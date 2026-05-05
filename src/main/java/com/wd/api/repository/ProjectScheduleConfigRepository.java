package com.wd.api.repository;

import com.wd.api.model.scheduling.ProjectScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectScheduleConfigRepository extends JpaRepository<ProjectScheduleConfig, Long> {
    Optional<ProjectScheduleConfig> findByProjectId(Long projectId);
}
