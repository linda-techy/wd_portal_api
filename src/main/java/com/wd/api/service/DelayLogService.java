package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.DelayLog;
import com.wd.api.model.PortalUser;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DelayLogRepository;
import com.wd.api.repository.PortalUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
