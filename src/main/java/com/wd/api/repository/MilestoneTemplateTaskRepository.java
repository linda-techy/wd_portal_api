package com.wd.api.repository;

import com.wd.api.model.MilestoneTemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneTemplateTaskRepository extends JpaRepository<MilestoneTemplateTask, Long> {

    List<MilestoneTemplateTask> findByMilestoneTemplateIdOrderByTaskOrderAsc(Long milestoneTemplateId);
}
