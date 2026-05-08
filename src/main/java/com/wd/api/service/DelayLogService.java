package com.wd.api.service;

import com.wd.api.dto.DelayLogSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.scheduling.DelayApplier;
import com.wd.api.service.scheduling.HandoverShiftDetector;
import com.wd.api.service.scheduling.HolidayService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DelayLogService {

    @Autowired
    private DelayLogRepository delayLogRepository;

    @Autowired
    private CustomerProjectRepository projectRepository;

    @Autowired
    private PortalUserRepository portalUserRepository;

    @Autowired
    private WebhookPublisherService webhookPublisherService;

    // ── S3 PR3 — apply delay → recompute CPM → alert handover shift ──────
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private CpmService cpmService;

    @Autowired
    private HandoverShiftDetector handoverShiftDetector;

    @Autowired
    private ProjectScheduleConfigRepository scheduleConfigRepo;

    @Transactional(readOnly = true)
    public Page<DelayLog> searchDelayLogs(DelayLogSearchFilter filter) {
        Specification<DelayLog> spec = buildSpecification(filter);
        return delayLogRepository.findAll(spec, filter.toPageable());
    }

    private Specification<DelayLog> buildSpecification(DelayLogSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across delayType, reason, description, logger name
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("delayType")), searchPattern),
                        cb.like(cb.lower(root.get("reason")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern),
                        cb.like(cb.lower(root.join("loggedBy").get("firstName")), searchPattern),
                        cb.like(cb.lower(root.join("loggedBy").get("lastName")), searchPattern)));
            }

            // Filter by projectId
            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }

            // Filter by delayType
            if (filter.getDelayType() != null && !filter.getDelayType().isEmpty()) {
                predicates.add(cb.equal(root.get("delayType"), filter.getDelayType()));
            }

            // Filter by loggedById
            if (filter.getLoggedById() != null) {
                predicates.add(cb.equal(root.get("loggedBy").get("id"), filter.getLoggedById()));
            }

            // Filter by severity (if field exists)
            if (filter.getSeverity() != null && !filter.getSeverity().isEmpty()) {
                predicates.add(cb.equal(root.get("severity"), filter.getSeverity()));
            }

            // Filter by active only (toDate is null)
            if (filter.isActiveOnly()) {
                predicates.add(cb.isNull(root.get("toDate")));
            }

            // Filter by closed only (toDate is not null)
            if (filter.isClosedOnly()) {
                predicates.add(cb.isNotNull(root.get("toDate")));
            }

            // Filter by status (from base class)
            if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
                if ("ACTIVE".equalsIgnoreCase(filter.getStatus())) {
                    predicates.add(cb.isNull(root.get("toDate")));
                } else if ("CLOSED".equalsIgnoreCase(filter.getStatus())) {
                    predicates.add(cb.isNotNull(root.get("toDate")));
                }
            }

            // Date range filter on fromDate
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fromDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fromDate"), filter.getEndDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<DelayLog> getDelaysByProject(Long projectId) {
        if (projectId == null)
            return java.util.Collections.emptyList();
        return delayLogRepository.findByProjectIdOrderByFromDateDesc(projectId);
    }

    @Transactional
    public DelayLog logDelay(DelayLog delay, Long projectId, Long userId) {
        if (projectId == null)
            throw new IllegalArgumentException("Project ID cannot be null");
        CustomerProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        delay.setProject(project);

        if (userId != null) {
            PortalUser user = portalUserRepository.findById(userId).orElse(null);
            delay.setLoggedBy(user);
            delay.setReportedBy(userId);
        }

        DelayLog saved = delayLogRepository.save(delay);

        // Publish webhook so Customer API can notify project customers
        String category = saved.getReasonCategory() != null ? saved.getReasonCategory() : saved.getDelayType();
        webhookPublisherService.publishDelayReported(projectId, saved.getId(), category);

        // S3 PR3 — apply delay to pending tasks, recompute CPM, then alert
        // customer if the handover date moved beyond the threshold.
        try {
            newApplier(projectId).applyDelayToTasks(saved);
            cpmService.recompute(projectId);
            handoverShiftDetector.checkAndAlert(projectId);
        } catch (Exception ex) {
            // Recompute / alert failures must never roll back the delay write.
            LoggerFactory.getLogger(DelayLogService.class)
                    .error("CPM/handover hook failed after logDelay: project={} delay={}",
                            projectId, saved.getId(), ex);
        }

        return saved;
    }

    @Transactional
    public DelayLog closeDelay(Long delayId, LocalDate endDate) {
        if (delayId == null)
            throw new IllegalArgumentException("Delay ID cannot be null");
        DelayLog existing = delayLogRepository.findById(delayId)
                .orElseThrow(() -> new IllegalArgumentException("Delay log not found: " + delayId));

        if (endDate.isBefore(existing.getFromDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        existing.setToDate(endDate);
        DelayLog saved = delayLogRepository.save(existing);

        // S3 PR3 — closing a delay records that the delay window has ended; it
        // must NOT re-apply the delay duration to PENDING tasks. logDelay
        // already shifted them when the delay was first recorded — re-applying
        // the same durationDays here would silently double-shift in the common
        // UI flow (POST /delay-logs with durationDays=5, then close).
        // CPM recompute still runs so any independent date changes propagate,
        // and the handover-shift detector still alerts customers if needed.
        Long projectId = existing.getProject() != null ? existing.getProject().getId() : null;
        if (projectId != null) {
            try {
                cpmService.recompute(projectId);
                handoverShiftDetector.checkAndAlert(projectId);
            } catch (Exception ex) {
                LoggerFactory.getLogger(DelayLogService.class)
                        .error("CPM/handover hook failed after closeDelay: project={} delay={}",
                                projectId, existing.getId(), ex);
            }
        }

        return saved;
    }

    @Transactional
    public void deleteDelay(Long delayId) {
        if (delayId != null) {
            delayLogRepository.deleteById(delayId);
        }
    }

    public Map<String, Object> getDelaySummary(Long projectId) {
        if (projectId == null)
            return Map.of();
        List<DelayLog> delays = delayLogRepository.findByProjectId(projectId);

        long totalDelays = delays.size();

        long totalDaysLost = delays.stream()
                .filter(d -> d.getToDate() != null)
                .mapToLong(d -> ChronoUnit.DAYS.between(d.getFromDate(), d.getToDate()) + 1)
                .sum();

        // Breakdown by reason_category (fall back to delayType when category is null)
        Map<String, Long> breakdownByCategory = delays.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getReasonCategory() != null ? d.getReasonCategory() : d.getDelayType(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDelays", totalDelays);
        summary.put("totalDaysLost", totalDaysLost);
        summary.put("breakdownByCategory", breakdownByCategory);
        return summary;
    }

    public Map<String, Long> getDelayImpactAnalysis(Long projectId) {
        if (projectId == null)
            return Map.of();
        List<DelayLog> delays = delayLogRepository.findByProjectId(projectId);

        // Group by Delay Type and sum duration
        return delays.stream()
                .filter(d -> d.getToDate() != null)
                .collect(Collectors.groupingBy(
                        DelayLog::getDelayType,
                        Collectors.summingLong(d -> ChronoUnit.DAYS.between(d.getFromDate(), d.getToDate()) + 1)));
    }

    // ── S3 PR3 — apply delay to pending tasks, recompute CPM, alert ─────────

    /**
     * Resolves the project's {@code sundayWorking} flag from
     * {@link com.wd.api.model.ProjectScheduleConfig} and constructs a
     * {@link DelayApplier} configured for that project.
     *
     * <p>S4 PR2 lifted {@code DelayApplier} to a top-level public class in
     * {@code service.scheduling} so {@code ChangeRequestMergeService} can
     * reuse the algorithm; this method continues to be the production entry
     * point for delay-driven shifts on the existing {@code logDelay} /
     * {@code closeDelay} hooks.
     */
    public DelayApplier newApplier(Long projectId) {
        boolean sundayWorking = scheduleConfigRepo.findByProjectId(projectId)
                .map(c -> Boolean.TRUE.equals(c.getSundayWorking()))
                .orElse(false);
        return new DelayApplier(taskRepository, holidayService, sundayWorking);
    }
}
