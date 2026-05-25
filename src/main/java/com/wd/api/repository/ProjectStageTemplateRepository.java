package com.wd.api.repository;

import com.wd.api.model.ProjectStageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectStageTemplateRepository extends JpaRepository<ProjectStageTemplate, Long> {

    List<ProjectStageTemplate> findByProjectIdOrderByStageNumber(Long projectId);

    @Modifying
    @Query("DELETE FROM ProjectStageTemplate t WHERE t.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
