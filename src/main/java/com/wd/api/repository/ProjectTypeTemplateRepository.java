package com.wd.api.repository;

import com.wd.api.model.ProjectTypeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTypeTemplateRepository extends JpaRepository<ProjectTypeTemplate, Long> {

    /**
     * Find template by project type name
     */
    Optional<ProjectTypeTemplate> findByProjectType(String projectType);

    /**
     * Find all templates by category
     */
    List<ProjectTypeTemplate> findByCategory(String category);

    /**
     * Check if template exists for project type
     */
    boolean existsByProjectType(String projectType);

    /**
     * Get all templates with their milestone templates
     */
    @Query("SELECT DISTINCT t FROM ProjectTypeTemplate t LEFT JOIN FETCH t.milestoneTemplates")
    List<ProjectTypeTemplate> findAllWithMilestones();

    /**
     * Get template with milestones by project type
     */
    @Query("SELECT t FROM ProjectTypeTemplate t LEFT JOIN FETCH t.milestoneTemplates WHERE t.projectType = :projectType")
    Optional<ProjectTypeTemplate> findByProjectTypeWithMilestones(String projectType);
}

