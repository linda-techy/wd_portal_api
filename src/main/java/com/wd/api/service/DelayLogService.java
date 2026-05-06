package com.wd.api.service;

import com.wd.api.dto.DelayLogSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.PortalUser;
import com.wd.api.model.Task;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.scheduling.HandoverShiftDetector;
import com.wd.api.service.scheduling.HolidayService;
import com.wd.api.service.scheduling.WorkingDayCalculator;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        return delayLogRepository.save(existing);
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
     * Applies a project-scoped delay's duration to every PENDING task on the
     * project: shifts both {@code startDate} and {@code endDate} forward by
     * {@code delay.durationDays} working days, preserving the task's original
     * working-day duration. Tasks already started, completed, or cancelled
     * are not mutated.
     *
     * <p>Package-private constructor is exposed for unit tests; production
     * code uses {@link #newApplier(Long)} which resolves the project's
     * {@code sundayWorking} flag from {@link ProjectScheduleConfig}.
     */
    static class DelayApplier {
        private final TaskRepository taskRepository;
        private final HolidayService holidayService;
        private final boolean sundayWorking;

        DelayApplier(TaskRepository taskRepository,
                     HolidayService holidayService,
                     boolean sundayWorking) {
            this.taskRepository = taskRepository;
            this.holidayService = holidayService;
            this.sundayWorking = sundayWorking;
        }

        void applyDelayToTasks(DelayLog delay) {
            if (delay == null || delay.getDurationDays() == null || delay.getDurationDays() <= 0) return;
            if (delay.getProject() == null || delay.getProject().getId() == null) return;
            Long projectId = delay.getProject().getId();
            int days = delay.getDurationDays();

            List<Task> tasks = taskRepository.findByProjectId(projectId);
            // Holiday window: from earliest task start to latest task end + days buffer.
            LocalDate winStart = tasks.stream()
                    .map(Task::getStartDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(LocalDate.now());
            LocalDate winEnd = tasks.stream()
                    .map(Task::getEndDate)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(winStart)
                    .plusDays(Math.max(days, 30) * 2L);
            Set<LocalDate> holidays = holidayService.holidaysFor(projectId, winStart, winEnd);

            for (Task t : tasks) {
                if (t.getStatus() != Task.TaskStatus.PENDING) continue;
                if (t.getStartDate() == null || t.getEndDate() == null) continue;

                int origDuration = WorkingDayCalculator
                        .workingDaysBetween(t.getStartDate(), t.getEndDate(), holidays, sundayWorking);
                LocalDate newStart = WorkingDayCalculator
                        .addWorkingDays(t.getStartDate(), days, holidays, sundayWorking);
                LocalDate newEnd = WorkingDayCalculator
                        .addWorkingDays(newStart, origDuration, holidays, sundayWorking);
                t.setStartDate(newStart);
                t.setEndDate(newEnd);
                taskRepository.save(t);
            }
        }
    }

    private DelayApplier newApplier(Long projectId) {
        boolean sundayWorking = scheduleConfigRepo.findByProjectId(projectId)
                .map(c -> Boolean.TRUE.equals(c.getSundayWorking()))
                .orElse(false);
        return new DelayApplier(taskRepository, holidayService, sundayWorking);
    }
}
