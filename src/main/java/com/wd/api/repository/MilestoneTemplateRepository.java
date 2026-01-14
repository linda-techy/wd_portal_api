package com.wd.api.repository;

import com.wd.api.model.MilestoneTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneTemplateRepository extends JpaRepository<MilestoneTemplate, Long> {

    /**
     * Find all milestones for a template, ordered by milestone_order
     */
    List<MilestoneTemplate> findByTemplateIdOrderByMilestoneOrderAsc(Long templateId);

    /**
     * Find milestones by phase
     */
    List<MilestoneTemplate> findByPhase(String phase);

    /**
     * Find milestones by template and phase
     */
    List<MilestoneTemplate> findByTemplateIdAndPhaseOrderByMilestoneOrderAsc(Long templateId, String phase);

    /**
     * Count milestones for a template
     */
    long countByTemplateId(Long templateId);

    /**
     * Get sum of default percentages for a template (should be 100)
     */
    @Query("SELECT SUM(mt.defaultPercentage) FROM MilestoneTemplate mt WHERE mt.template.id = :templateId")
    Double sumDefaultPercentagesByTemplateId(Long templateId);
}

