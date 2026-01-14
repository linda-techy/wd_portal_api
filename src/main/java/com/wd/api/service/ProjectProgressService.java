package com.wd.api.service;

import com.wd.api.dto.MilestoneTemplateDTO;
import com.wd.api.dto.ProjectProgressDTO;
import com.wd.api.dto.ProjectTypeTemplateDTO;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating and managing project progress
 * Implements hybrid progress tracking based on milestones, tasks, and budget
 */
@Service
@Transactional
public class ProjectProgressService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectProgressService.class);

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Autowired
    private ProjectMilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectProgressLogRepository progressLogRepository;

    @Autowired
    private ProjectTypeTemplateRepository typeTemplateRepository;

    @Autowired
    private MilestoneTemplateRepository milestoneTemplateRepository;

    /**
     * Calculate overall project progress using hybrid method
     * Formula: Overall = (Milestone × 0.40) + (Task × 0.30) + (Budget × 0.30)
     */
    public ProjectProgressDTO calculateProjectProgress(Long projectId) {
        logger.info("Calculating progress for project ID: {}", projectId);

        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

        // Get weights (default: 40-30-30)
        BigDecimal milestoneWeight = project.getMilestoneWeight() != null ? project.getMilestoneWeight() : new BigDecimal("0.40");
        BigDecimal taskWeight = project.getTaskWeight() != null ? project.getTaskWeight() : new BigDecimal("0.30");
        BigDecimal budgetWeight = project.getBudgetWeight() != null ? project.getBudgetWeight() : new BigDecimal("0.30");

        // Calculate individual progress components
        BigDecimal milestoneProgress = calculateMilestoneProgress(projectId);
        BigDecimal taskProgress = calculateTaskProgress(projectId);
        BigDecimal budgetProgress = calculateBudgetProgress(projectId);

        // Calculate weighted overall progress
        BigDecimal overallProgress = milestoneProgress.multiply(milestoneWeight)
                .add(taskProgress.multiply(taskWeight))
                .add(budgetProgress.multiply(budgetWeight))
                .setScale(2, RoundingMode.HALF_UP);

        // Build DTO
        ProjectProgressDTO dto = new ProjectProgressDTO();
        dto.setProjectId(projectId);
        dto.setOverallProgress(overallProgress);
        dto.setMilestoneProgress(milestoneProgress);
        dto.setTaskProgress(taskProgress);
        dto.setBudgetProgress(budgetProgress);
        dto.setLastUpdate(LocalDateTime.now());
        dto.setCalculationMethod(project.getProgressCalculationMethod());
        dto.setMilestoneWeight(milestoneWeight);
        dto.setTaskWeight(taskWeight);
        dto.setBudgetWeight(budgetWeight);

        // Add counts
        List<ProjectMilestone> milestones = milestoneRepository.findByProjectId(projectId);
        dto.setTotalMilestones(milestones.size());
        dto.setCompletedMilestones((int) milestones.stream()
                .filter(m -> "COMPLETED".equals(m.getStatus()))
                .count());

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        dto.setTotalTasks(tasks.size());
        dto.setCompletedTasks((int) tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().name()) || "DONE".equals(t.getStatus().name()))
                .count());

        dto.setTotalBudget(project.getBudget());
        dto.setSpentAmount(calculateTotalSpent(projectId));

        logger.info("Progress calculation completed for project {}: Overall={}%, Milestone={}%, Task={}%, Budget={}%",
                projectId, overallProgress, milestoneProgress, taskProgress, budgetProgress);

        return dto;
    }

    /**
     * Calculate progress based on milestone completion
     * Weighted average of completed milestone percentages
     */
    private BigDecimal calculateMilestoneProgress(Long projectId) {
        List<ProjectMilestone> milestones = milestoneRepository.findByProjectId(projectId);

        if (milestones.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Calculate weighted sum of milestone completions
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;

        for (ProjectMilestone milestone : milestones) {
            BigDecimal weight = milestone.getWeightPercentage() != null 
                    ? milestone.getWeightPercentage() 
                    : milestone.getMilestonePercentage();

            if (weight == null) {
                weight = new BigDecimal("100").divide(new BigDecimal(milestones.size()), 2, RoundingMode.HALF_UP);
            }

            BigDecimal completion = milestone.getCompletionPercentage() != null 
                    ? milestone.getCompletionPercentage() 
                    : ("COMPLETED".equals(milestone.getStatus()) ? new BigDecimal("100") : BigDecimal.ZERO);

            totalWeight = totalWeight.add(weight);
            weightedSum = weightedSum.add(weight.multiply(completion).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return weightedSum.multiply(new BigDecimal("100"))
                .divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate progress based on task completion
     * Simple percentage of completed tasks
     */
    private BigDecimal calculateTaskProgress(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);

        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long completedCount = tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().name()) || "DONE".equals(t.getStatus().name()))
                .count();

        return new BigDecimal(completedCount)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(tasks.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate progress based on budget utilization
     * Compares spent amount to total budget
     */
    private BigDecimal calculateBudgetProgress(Long projectId) {
        CustomerProject project = projectRepository.findById(projectId).orElse(null);
        if (project == null || project.getBudget() == null || project.getBudget().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalSpent = calculateTotalSpent(projectId);
        BigDecimal progress = totalSpent.multiply(new BigDecimal("100"))
                .divide(project.getBudget(), 2, RoundingMode.HALF_UP);

        // Cap at 100%
        return progress.min(new BigDecimal("100"));
    }

    /**
     * Calculate total amount spent on project
     * Note: This is a simplified calculation. In production, you'd query from
     * payment_schedules or other financial tables linked to projects.
     */
    private BigDecimal calculateTotalSpent(Long projectId) {
        // TODO: Implement proper payment tracking when payment schema is clarified
        // For now, return zero as budget tracking will be based on other sources
        return BigDecimal.ZERO;
    }

    /**
     * Update project progress in database and log the change
     */
    public void updateProjectProgress(Long projectId, String changeType, String changeReason, Long changedBy) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Store previous values
        BigDecimal previousOverall = project.getOverallProgress();
        BigDecimal previousMilestone = project.getMilestoneProgress();
        BigDecimal previousTask = project.getTaskProgress();
        BigDecimal previousBudget = project.getBudgetProgress();

        // Calculate new progress
        ProjectProgressDTO newProgress = calculateProjectProgress(projectId);

        // Update project
        project.setOverallProgress(newProgress.getOverallProgress());
        project.setMilestoneProgress(newProgress.getMilestoneProgress());
        project.setTaskProgress(newProgress.getTaskProgress());
        project.setBudgetProgress(newProgress.getBudgetProgress());
        project.setLastProgressUpdate(LocalDateTime.now());

        projectRepository.save(project);

        // Log the change
        ProjectProgressLog log = new ProjectProgressLog();
        log.setProject(project);
        log.setPreviousProgress(previousOverall);
        log.setNewProgress(newProgress.getOverallProgress());
        log.setPreviousMilestoneProgress(previousMilestone);
        log.setNewMilestoneProgress(newProgress.getMilestoneProgress());
        log.setPreviousTaskProgress(previousTask);
        log.setNewTaskProgress(newProgress.getTaskProgress());
        log.setPreviousBudgetProgress(previousBudget);
        log.setNewBudgetProgress(newProgress.getBudgetProgress());
        log.setChangeType(changeType);
        log.setChangeReason(changeReason);
        // TODO: Set changedBy from security context

        progressLogRepository.save(log);

        logger.info("Project {} progress updated: {}% -> {}%", projectId, previousOverall, newProgress.getOverallProgress());
    }

    /**
     * Get all project type templates with their milestones
     */
    @Transactional(readOnly = true)
    public List<ProjectTypeTemplateDTO> getAllProjectTypeTemplates() {
        return typeTemplateRepository.findAllWithMilestones().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get template for specific project type
     */
    @Transactional(readOnly = true)
    public ProjectTypeTemplateDTO getTemplateByProjectType(String projectType) {
        return typeTemplateRepository.findByProjectTypeWithMilestones(projectType)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Create default milestones for a project based on its type
     */
    public void createMilestonesFromTemplate(Long projectId, String projectType) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectTypeTemplate template = typeTemplateRepository.findByProjectTypeWithMilestones(projectType)
                .orElseThrow(() -> new RuntimeException("Template not found for project type: " + projectType));

        List<MilestoneTemplate> templates = template.getMilestoneTemplates();
        
        logger.info("Creating {} milestones for project {} from template {}", templates.size(), projectId, projectType);

        for (MilestoneTemplate mt : templates) {
            ProjectMilestone milestone = new ProjectMilestone();
            milestone.setName(mt.getMilestoneName());
            milestone.setDescription(mt.getDescription());
            milestone.setMilestonePercentage(mt.getDefaultPercentage());
            milestone.setWeightPercentage(mt.getDefaultPercentage());
            milestone.setStatus("PENDING");
            milestone.setProject(project);
            milestone.setCreatedAt(LocalDateTime.now());

            milestoneRepository.save(milestone);
        }

        logger.info("Successfully created milestones for project {}", projectId);
    }

    /**
     * Get progress history for a project
     */
    @Transactional(readOnly = true)
    public List<ProjectProgressLog> getProgressHistory(Long projectId) {
        return progressLogRepository.findByProjectIdOrderByChangedAtDesc(projectId);
    }

    // Helper method to convert entity to DTO
    private ProjectTypeTemplateDTO convertToDTO(ProjectTypeTemplate entity) {
        ProjectTypeTemplateDTO dto = new ProjectTypeTemplateDTO();
        dto.setId(entity.getId());
        dto.setProjectType(entity.getProjectType());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());

        if (entity.getMilestoneTemplates() != null) {
            List<MilestoneTemplateDTO> milestoneDTOs = entity.getMilestoneTemplates().stream()
                    .map(this::convertMilestoneToDTO)
                    .collect(Collectors.toList());
            dto.setMilestoneTemplates(milestoneDTOs);
            dto.setTotalMilestones(milestoneDTOs.size());

            int totalDays = milestoneDTOs.stream()
                    .filter(m -> m.getEstimatedDurationDays() != null)
                    .mapToInt(MilestoneTemplateDTO::getEstimatedDurationDays)
                    .sum();
            dto.setEstimatedTotalDays(totalDays);
        }

        return dto;
    }

    private MilestoneTemplateDTO convertMilestoneToDTO(MilestoneTemplate entity) {
        MilestoneTemplateDTO dto = new MilestoneTemplateDTO();
        dto.setId(entity.getId());
        dto.setTemplateId(entity.getTemplate().getId());
        dto.setMilestoneName(entity.getMilestoneName());
        dto.setMilestoneOrder(entity.getMilestoneOrder());
        dto.setDefaultPercentage(entity.getDefaultPercentage());
        dto.setDescription(entity.getDescription());
        dto.setPhase(entity.getPhase());
        dto.setEstimatedDurationDays(entity.getEstimatedDurationDays());
        return dto;
    }
}

