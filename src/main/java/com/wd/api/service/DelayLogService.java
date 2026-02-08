package com.wd.api.service;

import com.wd.api.dto.DelayLogSearchFilter;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
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
        }

        return delayLogRepository.save(delay);
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
}
