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
 * Service for calculating and managing project progress.
 *
 * <p>S3 PR1 rewrite: replaces the legacy hybrid 40/30/30 algorithm
 * (milestone × 0.40 + task × 0.30 + budget × 0.30) with a single
 * task-weight-based percentage. See
 * {@link #calculateProjectProgress(Long)} for the formula.
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

        /**
         * Calculate overall project progress using weight-based formula.
         *
         * <p>S3 PR1 rewrite: the legacy hybrid 40/30/30 (milestone × 0.40 +
         * task × 0.30 + budget × 0.30) was replaced with a single
         * task-weight-based percentage:
         *
         * <pre>
         *   effectiveWeight(t) = t.weight ?? t.durationDays ?? 1
         *   total              = sum effectiveWeight over all tasks
         *   completed          = sum effectiveWeight over completed tasks
         *   pct                = completed / total × 100   (HALF_UP @ 2dp)
         * </pre>
         *
         * Empty project = 0%. CANCELLED tasks are filtered entirely —
         * they contribute to neither numerator nor denominator (cancelled
         * scope must not penalize progress). A task counts as completed
         * when its status is COMPLETED; PR2 will tighten this to require
         * photo evidence before status can reach COMPLETED.
         */
        public ProjectProgressDTO calculateProjectProgress(Long projectId) {
                logger.info("Calculating progress for project ID: {}", projectId);

                CustomerProject project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

                List<Task> tasks = taskRepository.findByProjectId(projectId);

                ProjectProgressDTO dto = new ProjectProgressDTO();
                dto.setProjectId(projectId);
                dto.setLastUpdate(LocalDateTime.now());
                dto.setCalculationMethod("WEIGHTED_TASK");
                dto.setTotalBudget(project.getBudget());
                dto.setSpentAmount(BigDecimal.ZERO);
                // Pre-PR1 callers (DashboardService.budgetUtilizationPct)
                // read CustomerProject.milestoneProgress / .budgetProgress
                // as non-null. The new weighted algorithm doesn't compute
                // milestone/budget components, so ZERO is the
                // semantically-correct legacy-field value while preserving
                // the non-null contract.
                dto.setMilestoneProgress(BigDecimal.ZERO);
                dto.setBudgetProgress(BigDecimal.ZERO);

                if (tasks.isEmpty()) {
                        dto.setOverallProgress(BigDecimal.ZERO);
                        dto.setTaskProgress(BigDecimal.ZERO);
                        dto.setTotalTasks(0);
                        dto.setCompletedTasks(0);
                        dto.setTotalMilestones(0);
                        dto.setCompletedMilestones(0);
                        return dto;
                }

                long totalWeight = 0L;
                long completedWeight = 0L;
                int completedCount = 0;
                int activeCount = 0;
                for (Task t : tasks) {
                        // Spec: CANCELLED tasks don't count toward progress
                        // (no work done) and aren't denominators either —
                        // filter them out entirely.
                        if (t.getStatus() == Task.TaskStatus.CANCELLED) continue;
                        int w = effectiveWeight(t);
                        totalWeight += w;
                        activeCount++;
                        if (isCompleted(t)) {
                                completedWeight += w;
                                completedCount++;
                        }
                }

                if (totalWeight == 0L) {
                        // All tasks are cancelled — treat like an empty
                        // project: 0% with no active denominator.
                        dto.setOverallProgress(BigDecimal.ZERO);
                        dto.setTaskProgress(BigDecimal.ZERO);
                        dto.setTotalTasks(0);
                        dto.setCompletedTasks(0);
                        dto.setTotalMilestones(0);
                        dto.setCompletedMilestones(0);
                        return dto;
                }

                BigDecimal pct = BigDecimal.valueOf(completedWeight)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);

                dto.setOverallProgress(pct);
                dto.setTaskProgress(pct);
                dto.setTotalTasks(activeCount);
                dto.setCompletedTasks(completedCount);
                dto.setTotalMilestones(0);
                dto.setCompletedMilestones(0);

                logger.info("Progress for project {}: {}% ({}/{} tasks completed by weight)",
                                projectId, pct, completedCount, activeCount);

                return dto;
        }

        private static int effectiveWeight(Task t) {
                if (t.getWeight() != null) return t.getWeight();
                if (t.getDurationDays() != null) return t.getDurationDays();
                return 1;
        }

        private static boolean isCompleted(Task t) {
                if (t.getStatus() == null) return false;
                String name = t.getStatus().name();
                return "COMPLETED".equals(name) || "DONE".equals(name);
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

                progressLogRepository.save(log);

                logger.info("Project {} progress updated: {}% -> {}%", projectId, previousOverall,
                                newProgress.getOverallProgress());
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
                                .orElseThrow(() -> new RuntimeException(
                                                "Template not found for project type: " + projectType));

                List<MilestoneTemplate> templates = template.getMilestoneTemplates();

                logger.info("Creating {} milestones for project {} from template {}", templates.size(), projectId,
                                projectType);

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
