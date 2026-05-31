package com.wd.api.dto;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.MilestoneTemplate;
import com.wd.api.model.ProjectMilestone;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectMilestoneRequest(
        CustomerProject project, String name, String description, BigDecimal milestonePercentage,
        BigDecimal amount, String status, LocalDate dueDate, LocalDate completedDate,
        MilestoneTemplate template, BigDecimal completionPercentage, BigDecimal weightPercentage,
        String progressSource, LocalDate actualStartDate, LocalDate actualEndDate) {
    public ProjectMilestone toEntity() {
        ProjectMilestone m = new ProjectMilestone();
        m.setProject(project); m.setName(name); m.setDescription(description);
        m.setMilestonePercentage(milestonePercentage); m.setAmount(amount); m.setStatus(status);
        m.setDueDate(dueDate); m.setCompletedDate(completedDate); m.setTemplate(template);
        m.setCompletionPercentage(completionPercentage); m.setWeightPercentage(weightPercentage);
        m.setProgressSource(progressSource); m.setActualStartDate(actualStartDate);
        m.setActualEndDate(actualEndDate);
        return m;
    }
}
