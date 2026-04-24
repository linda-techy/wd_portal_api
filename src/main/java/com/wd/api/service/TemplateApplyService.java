package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TemplateApplyService {

    public record Result(boolean created, int milestoneCount) {}

    private final ProjectMilestoneRepository milestoneRepo;
    private final TaskRepository taskRepo;
    private final ProjectTypeTemplateRepository templateRepo;
    private final MilestoneTemplateRepository milestoneTemplateRepo;
    private final MilestoneTemplateTaskRepository milestoneTaskRepo;
    private final CustomerProjectRepository projectRepo;

    public TemplateApplyService(ProjectMilestoneRepository milestoneRepo,
                                 TaskRepository taskRepo,
                                 ProjectTypeTemplateRepository templateRepo,
                                 MilestoneTemplateRepository milestoneTemplateRepo,
                                 MilestoneTemplateTaskRepository milestoneTaskRepo,
                                 CustomerProjectRepository projectRepo) {
        this.milestoneRepo = milestoneRepo;
        this.taskRepo = taskRepo;
        this.templateRepo = templateRepo;
        this.milestoneTemplateRepo = milestoneTemplateRepo;
        this.milestoneTaskRepo = milestoneTaskRepo;
        this.projectRepo = projectRepo;
    }

    @Transactional
    public Result apply(Long projectId, String templateCode) {
        ProjectTypeTemplate template = templateRepo.findByCode(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));

        long existingCount = milestoneRepo.countByProjectId(projectId);
        if (existingCount > 0) {
            return new Result(false, (int) existingCount);
        }

        CustomerProject project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<MilestoneTemplate> milestoneTemplates =
                milestoneTemplateRepo.findByTemplateIdOrderByMilestoneOrderAsc(template.getId());
        int created = 0;
        for (MilestoneTemplate mt : milestoneTemplates) {
            if ("G_PLUS_N".equals(templateCode)
                    && mt.getMilestoneName() != null
                    && mt.getMilestoneName().startsWith("Floor ")) {
                int floors = project.getFloors() != null ? project.getFloors() : 1;
                for (int f = 0; f <= floors; f++) {
                    String suffix;
                    if (f == 0) suffix = "Ground";
                    else if (f == 1) suffix = "1st Floor";
                    else if (f == 2) suffix = "2nd Floor";
                    else if (f == 3) suffix = "3rd Floor";
                    else suffix = f + "th Floor";
                    instantiate(mt, project, "[" + suffix + "] " + mt.getMilestoneName());
                    created++;
                }
            } else {
                instantiate(mt, project, mt.getMilestoneName());
                created++;
            }
        }
        return new Result(true, created);
    }

    private void instantiate(MilestoneTemplate template, CustomerProject project, String name) {
        ProjectMilestone milestone = ProjectMilestone.builder()
                .project(project)
                .name(name)
                .description(template.getDescription())
                .weightPercentage(template.getDefaultPercentage())
                .amount(BigDecimal.ZERO)
                .status("PENDING")
                .completionPercentage(BigDecimal.ZERO)
                .progressSource("COMPUTED")
                .template(template)
                .build();
        ProjectMilestone saved = milestoneRepo.save(milestone);

        for (MilestoneTemplateTask tt :
                milestoneTaskRepo.findByMilestoneTemplateIdOrderByTaskOrderAsc(template.getId())) {
            Task t = new Task();
            t.setTitle(tt.getTaskName());
            t.setProject(project);
            t.setStatus(Task.TaskStatus.PENDING);
            t.setPriority(Task.TaskPriority.MEDIUM);
            t.setMilestoneId(saved.getId());
            // Stamp planned start/end based on estimatedDays so the customer
            // timeline bucket queries (which require non-null dates) include
            // freshly-templated tasks. PMs can adjust later in the Gantt screen.
            int days = tt.getEstimatedDays() != null ? tt.getEstimatedDays() : 7;
            LocalDate start = LocalDate.now();
            t.setStartDate(start);
            t.setEndDate(start.plusDays(days));
            t.setDueDate(start.plusDays(days));  // dueDate stays as plan-end too
            t.setProgressPercent(0);
            taskRepo.save(t);
        }
    }
}
