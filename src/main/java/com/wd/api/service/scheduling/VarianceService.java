package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectBaseline;
import com.wd.api.model.scheduling.TaskBaseline;
import com.wd.api.repository.ProjectBaselineRepository;
import com.wd.api.repository.TaskBaselineRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.VarianceRowDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VarianceService {

    private final ProjectBaselineRepository projectBaselineRepo;
    private final TaskBaselineRepository taskBaselineRepo;
    private final TaskRepository taskRepo;

    public VarianceService(ProjectBaselineRepository projectBaselineRepo,
                           TaskBaselineRepository taskBaselineRepo,
                           TaskRepository taskRepo) {
        this.projectBaselineRepo = projectBaselineRepo;
        this.taskBaselineRepo = taskBaselineRepo;
        this.taskRepo = taskRepo;
    }

    @Transactional(readOnly = true)
    public List<VarianceRowDto> computeFor(Long projectId) {
        ProjectBaseline baseline = projectBaselineRepo.findByProjectId(projectId)
                .orElseThrow(() -> new NoBaselineException(projectId));

        // Single-pass: load all tasks + all task-baseline rows; index baselines by taskId.
        List<Task> tasks = taskRepo.findByProjectId(projectId);
        Map<Long, TaskBaseline> baselineByTaskId = new HashMap<>();
        for (TaskBaseline tb : taskBaselineRepo.findByBaselineId(baseline.getId())) {
            baselineByTaskId.put(tb.getTaskId(), tb);
        }

        return tasks.stream()
                .map(t -> toRow(t, baselineByTaskId.get(t.getId())))
                .toList();
    }

    private VarianceRowDto toRow(Task t, TaskBaseline tb) {
        LocalDate baseStart = tb == null ? null : tb.getBaselineStart();
        LocalDate baseEnd   = tb == null ? null : tb.getBaselineEnd();
        Integer planVsBase  = (baseEnd == null || t.getEfDate() == null)
                ? null
                : (int) ChronoUnit.DAYS.between(baseEnd, t.getEfDate());
        Integer actualVsPlan = (t.getActualEndDate() == null || t.getEfDate() == null)
                ? null
                : (int) ChronoUnit.DAYS.between(t.getEfDate(), t.getActualEndDate());
        Integer actualVsBase = (t.getActualEndDate() == null || baseEnd == null)
                ? null
                : (int) ChronoUnit.DAYS.between(baseEnd, t.getActualEndDate());

        return new VarianceRowDto(
                t.getId(),
                t.getTitle(),
                baseStart,
                baseEnd,
                t.getEsDate(),
                t.getEfDate(),
                t.getActualStartDate(),
                t.getActualEndDate(),
                planVsBase,
                actualVsPlan,
                actualVsBase,
                Boolean.TRUE.equals(t.getIsCritical()));
    }
}
