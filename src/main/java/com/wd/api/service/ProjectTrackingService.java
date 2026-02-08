package com.wd.api.service;

import com.wd.api.model.*;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectTrackingService {

    private final ProjectPhaseRepository phaseRepository;
    private final DelayLogRepository delayLogRepository;
    private final ProjectVariationRepository variationRepository;
    private final CustomerProjectRepository projectRepository;
    private final PortalUserRepository userRepository;

    // ===== PROJECT PHASES =====

    public List<ProjectPhase> getProjectPhases(Long projectId) {
        return phaseRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
    }

    @Transactional
    @SuppressWarnings("null")
    public ProjectPhase createPhase(Long projectId, String phaseName, LocalDate plannedStart,
            LocalDate plannedEnd, Integer displayOrder) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectPhase phase = ProjectPhase.builder()
                .project(project)
                .phaseName(phaseName)
                .plannedStart(plannedStart)
                .plannedEnd(plannedEnd)
                .displayOrder(displayOrder)
                .status("NOT_STARTED")
                .build();

        return phaseRepository.save(phase);
    }

    @Transactional
    @SuppressWarnings("null")
    public ProjectPhase updatePhaseProgress(Long phaseId, String status,
            LocalDate actualStart, LocalDate actualEnd) {
        ProjectPhase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Phase not found"));

        if (status != null)
            phase.setStatus(status);
        if (actualStart != null)
            phase.setActualStart(actualStart);
        if (actualEnd != null)
            phase.setActualEnd(actualEnd);

        // Auto-detect delay status
        if ("IN_PROGRESS".equals(status) && phase.getPlannedEnd() != null
                && LocalDate.now().isAfter(phase.getPlannedEnd())) {
            phase.setStatus("DELAYED");
        }

        return phaseRepository.save(phase);
    }

    // ===== DELAY LOGS =====

    public List<DelayLog> getDelayLogs(Long projectId) {
        return delayLogRepository.findByProjectIdOrderByFromDateDesc(projectId);
    }

    @Transactional
    @SuppressWarnings("null")
    public DelayLog logDelay(Long projectId, Long phaseId, String delayType,
            LocalDate fromDate, LocalDate toDate, String reason, Long loggedById) {
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectPhase phase = phaseId != null ? phaseRepository.findById(phaseId).orElse(null) : null;
        PortalUser loggedBy = loggedById != null ? userRepository.findById(loggedById).orElse(null) : null;

        DelayLog log = DelayLog.builder()
                .project(project)
                .phase(phase)
                .delayType(delayType)
                .fromDate(fromDate)
                .toDate(toDate)
                .reasonText(reason)
                .loggedBy(loggedBy)
                .build();

        return delayLogRepository.save(log);
    }

    // ===== PROJECT VARIATIONS (CHANGE ORDERS) =====

    public List<ProjectVariation> getVariations(Long projectId) {
        return variationRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional
    @SuppressWarnings("null")
    public ProjectVariation createVariation(Long projectId, String description,
            BigDecimal estimatedAmount, Long createdById) {
        Long pId = java.util.Objects.requireNonNull(projectId);
        CustomerProject project = projectRepository.findById(pId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectVariation variation = ProjectVariation.builder()
                .project(project)
                .description(description)
                .estimatedAmount(estimatedAmount)
                .status(VariationStatus.DRAFT)
                .build();
        if (createdById != null) {
            variation.setCreatedByUserId(createdById);
        }

        ProjectVariation savedVariation = variationRepository.save(variation);
        return java.util.Objects.requireNonNull(savedVariation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ProjectVariation submitForApproval(Long variationId) {
        Long vId = java.util.Objects.requireNonNull(variationId);
        ProjectVariation variation = variationRepository.findById(vId)
                .orElseThrow(() -> new RuntimeException("Variation not found"));

        variation.setStatus(VariationStatus.PENDING_APPROVAL);
        ProjectVariation savedVariation = variationRepository.save(variation);
        return java.util.Objects.requireNonNull(savedVariation);
    }

    @Transactional
    @SuppressWarnings("null")
    public ProjectVariation approveVariation(Long variationId, Long approvedById, boolean approve) {
        Long vId = java.util.Objects.requireNonNull(variationId);
        ProjectVariation variation = variationRepository.findById(vId)
                .orElseThrow(() -> new RuntimeException("Variation not found"));

        Long aId = java.util.Objects.requireNonNull(approvedById);
        PortalUser approvedBy = userRepository.findById(aId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        variation.setApprovedBy(approvedBy);
        variation.setApprovedAt(LocalDateTime.now());
        variation.setClientApproved(approve);
        variation.setStatus(approve ? VariationStatus.APPROVED : VariationStatus.REJECTED);

        ProjectVariation savedVariation = variationRepository.save(variation);
        return java.util.Objects.requireNonNull(savedVariation);
    }

    // ===== PROJECT HEALTH METRICS =====

    public ProjectHealthSummary getProjectHealth(Long projectId) {
        List<ProjectPhase> phases = phaseRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        List<DelayLog> delays = delayLogRepository.findByProjectIdOrderByFromDateDesc(projectId);
        List<ProjectVariation> variations = variationRepository.findByProjectIdAndStatus(projectId,
                VariationStatus.APPROVED);

        int totalPhases = phases.size();
        int completedPhases = (int) phases.stream().filter(p -> "COMPLETED".equals(p.getStatus())).count();
        int delayedPhases = (int) phases.stream().filter(p -> "DELAYED".equals(p.getStatus())).count();

        BigDecimal totalVariationAmount = variations.stream()
                .map(ProjectVariation::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String overallStatus = calculateOverallStatus(delayedPhases, totalPhases);

        return new ProjectHealthSummary(
                totalPhases,
                completedPhases,
                delayedPhases,
                delays.size(),
                variations.size(),
                totalVariationAmount,
                overallStatus);
    }

    private String calculateOverallStatus(int delayedPhases, int totalPhases) {
        if (totalPhases == 0)
            return "NOT_STARTED";
        double delayRatio = (double) delayedPhases / totalPhases;
        if (delayRatio >= 0.5)
            return "AT_RISK";
        if (delayedPhases > 0)
            return "MINOR_DELAYS";
        return "ON_TRACK";
    }

    // Inner class for health summary
    public record ProjectHealthSummary(
            int totalPhases,
            int completedPhases,
            int delayedPhases,
            int totalDelayLogs,
            int approvedVariations,
            BigDecimal variationAmount,
            String overallStatus) {
    }
}
