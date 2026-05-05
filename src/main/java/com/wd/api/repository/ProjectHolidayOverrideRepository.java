package com.wd.api.repository;

import com.wd.api.model.scheduling.ProjectHolidayOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ProjectHolidayOverrideRepository extends JpaRepository<ProjectHolidayOverride, Long> {
    List<ProjectHolidayOverride> findByProjectId(Long projectId);
    List<ProjectHolidayOverride> findByProjectIdAndOverrideDateBetween(Long projectId, LocalDate start, LocalDate end);
}
