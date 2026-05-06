package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.TaskBaseline;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.TaskBaselineRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.ApproveBaselineResponse;
import com.wd.api.service.scheduling.dto.ProjectBaselineDto;
import com.wd.api.service.scheduling.dto.TaskBaselineDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectBaselineService {

    private final ProjectBaselineRepository projectBaselineRepo;
    private final TaskBaselineRepository taskBaselineRepo;
    private final TaskRepository taskRepo;
    private final CpmService cpmService;

    public ProjectBaselineService(ProjectBaselineRepository projectBaselineRepo,
                                  TaskBaselineRepository taskBaselineRepo,
                                  TaskRepository taskRepo,
                                  CpmService cpmService) {
        this.projectBaselineRepo = projectBaselineRepo;
        this.taskBaselineRepo = taskBaselineRepo;
        this.taskRepo = taskRepo;
        this.cpmService = cpmService;
    }

    @Transactional
    public ApproveBaselineResponse approve(Long projectId, Long approvedBy) {
        if (projectBaselineRepo.existsByProjectId(projectId)) {
            throw new BaselineAlreadyExistsException(projectId);
        }

        // Recompute CPM FIRST so the snapshot captures CPM-derived ES/EF,
        // not stale plan-only dates.
        cpmService.recompute(projectId);

        List<Task> tasks = taskRepo.findByProjectId(projectId);
        if (tasks.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot baseline project " + projectId + ": project must have at least one task");
        }

        LocalDate projectStart = tasks.stream()
                .map(Task::getEsDate)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot baseline project " + projectId
                                + ": no tasks have CPM-computed ES dates"));
        LocalDate projectFinish = tasks.stream()
                .map(Task::getEfDate)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot baseline project " + projectId
                                + ": no tasks have CPM-computed EF dates"));

        ProjectBaseline saved = projectBaselineRepo.save(new ProjectBaseline(
                projectId, LocalDateTime.now(), approvedBy, projectStart, projectFinish));

        List<TaskBaseline> snapshots = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            int durationDays = (int) ChronoUnit.DAYS.between(t.getEsDate(), t.getEfDate());
            snapshots.add(new TaskBaseline(
                    saved.getId(),
                    t.getId(),
                    t.getTitle(),
                    t.getEsDate(),
                    t.getEfDate(),
                    durationDays));
        }
        taskBaselineRepo.saveAll(snapshots);

        return new ApproveBaselineResponse(
                saved.getId(), projectId, projectStart, projectFinish, snapshots.size());
    }

    @Transactional(readOnly = true)
    public ProjectBaselineDto getBaseline(Long projectId) {
        ProjectBaseline pb = projectBaselineRepo.findByProjectId(projectId)
                .orElseThrow(() -> new NoBaselineException(projectId));
        List<TaskBaselineDto> taskDtos = taskBaselineRepo.findByBaselineId(pb.getId()).stream()
                .map(tb -> new TaskBaselineDto(
                        tb.getId(),
                        tb.getTaskId(),
                        tb.getTaskNameAtBaseline(),
                        tb.getBaselineStart(),
                        tb.getBaselineEnd(),
                        tb.getBaselineDurationDays()))
                .toList();
        return new ProjectBaselineDto(
                pb.getId(),
                pb.getProjectId(),
                pb.getApprovedAt(),
                pb.getApprovedBy(),
                pb.getProjectStartDate(),
                pb.getProjectFinishDate(),
                taskDtos);
    }
}
