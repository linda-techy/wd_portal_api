package com.wd.api.repository.scheduling;

import com.wd.api.model.scheduling.WbsTemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WbsTemplateTaskRepository extends JpaRepository<WbsTemplateTask, Long> {

    List<WbsTemplateTask> findByPhaseIdOrderBySequenceAsc(Long phaseId);

    @Query("SELECT t FROM WbsTemplateTask t WHERE t.phase.template.id = :templateId " +
           "ORDER BY t.phase.sequence, t.sequence")
    List<WbsTemplateTask> findAllByTemplateIdOrdered(@Param("templateId") Long templateId);
}
