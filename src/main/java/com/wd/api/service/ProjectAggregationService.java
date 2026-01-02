package com.wd.api.service;

import com.wd.api.dto.ActivityFeedDTO;
import com.wd.api.dto.ProjectSummaryDTO;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectMember;
import com.wd.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectAggregationService {

        private final CustomerProjectRepository projectRepository;
        private final ProjectMemberRepository memberRepository;
        private final TaskRepository taskRepository;
        private final ActivityFeedService activityFeedService;
        private final DelayLogRepository delayLogRepository;
        // Finance repositories would be injected here (e.g.,
        // PaymentTransactionRepository)

        @Transactional(readOnly = true)
        public ProjectSummaryDTO getProjectSummary(Long projectId) {
                // 1. Fetch Project
                CustomerProject project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

                // 2. Fetch Team
                List<ProjectSummaryDTO.ProjectMemberDTO> team = memberRepository.findByProjectId(projectId).stream()
                                .map(m -> {
                                        Long userId = null;
                                        String name = "Unknown";

                                        if (m.getPortalUser() != null) {
                                                userId = m.getPortalUser().getId();
                                                name = m.getPortalUser().getFirstName() + " "
                                                                + m.getPortalUser().getLastName();
                                        } else if (m.getCustomerUser() != null) {
                                                userId = m.getCustomerUser().getId();
                                                name = m.getCustomerUser().getFirstName() + " "
                                                                + m.getCustomerUser().getLastName();
                                        }

                                        return ProjectSummaryDTO.ProjectMemberDTO.builder()
                                                        .userId(userId)
                                                        .name(name)
                                                        .role(m.getRoleInProject())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // 3. Execution Stats
                int totalTasks = taskRepository.countByProjectId(projectId);
                int completedTasks = taskRepository.countByProjectIdAndStatus(projectId, "COMPLETED");
                // Simplified overdue check - ideally DB query with date comparison
                int activeDelays = delayLogRepository.findByProjectId(projectId).size();

                ProjectSummaryDTO.ProjectExecutionStats stats = ProjectSummaryDTO.ProjectExecutionStats.builder()
                                .totalTasks(totalTasks)
                                .completedTasks(completedTasks)
                                .overdueTasks(0) // TODO: Implement overdue query
                                .activeDelays(activeDelays)
                                .build();

                // 4. Financial Snapshot (Mocked for now until Finance module is fully linked)
                ProjectSummaryDTO.FinancialSnapshot finance = ProjectSummaryDTO.FinancialSnapshot.builder()
                                .totalBudget(BigDecimal.ZERO)
                                .totalInvoiced(BigDecimal.ZERO)
                                .totalPaid(BigDecimal.ZERO)
                                .pendingPayments(BigDecimal.ZERO)
                                .build();

                // 5. Recent Activity
                List<ActivityFeedDTO> activities = activityFeedService
                                .getRecentProjectActivities(projectId)
                                .stream()
                                .map(a -> ActivityFeedDTO.builder()
                                                .id(a.getId())
                                                .title(a.getTitle())
                                                .description(a.getDescription())
                                                // .activityType(a.getActivityType().getName()) // Assuming relationship
                                                // exists
                                                .createdAt(a.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());

                return ProjectSummaryDTO.builder()
                                .project(project)
                                .teamMembers(team)
                                .executionStats(stats)
                                .financialSnapshot(finance)
                                .recentActivities(activities)
                                .build();
        }
}
