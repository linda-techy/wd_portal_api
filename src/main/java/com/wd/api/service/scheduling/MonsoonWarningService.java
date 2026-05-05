package com.wd.api.service.scheduling;

import com.wd.api.model.Task;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.scheduling.dto.MonsoonWarning;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lazily compute monsoon warnings for a project's tasks.
 *
 * <p>Per S1 design: a task is "monsoon-flagged" when {@code Task.monsoon_sensitive}
 * is true (set by the cloner from the WBS template, or manually edited). The
 * monsoon window is read from {@code ProjectScheduleConfig} (mmdd shorts) with
 * a default of Jun 1 → Sep 30 (Kerala SW monsoon).
 *
 * <p>For each flagged task with both start and end dates, we walk every year
 * between the start and end and report a single warning for the FIRST overlap
 * — collapsing multi-year spans (rare in practice). The severity is
 * {@code OVERLAP_FULL} when the task fits entirely inside the window;
 * {@code OVERLAP_PARTIAL} otherwise.
 */
@Service
public class MonsoonWarningService {

    static final MonthDay DEFAULT_MONSOON_START = MonthDay.of(6, 1);
    static final MonthDay DEFAULT_MONSOON_END   = MonthDay.of(9, 30);

    private final TaskRepository tasks;
    private final ProjectScheduleConfigRepository configs;

    public MonsoonWarningService(TaskRepository tasks, ProjectScheduleConfigRepository configs) {
        this.tasks = tasks;
        this.configs = configs;
    }

    @Transactional(readOnly = true)
    public List<MonsoonWarning> warningsFor(Long projectId) {
        Optional<ProjectScheduleConfig> cfg = configs.findByProjectId(projectId);
        MonthDay mStart = cfg.map(c -> mmddToMonthDay(c.getMonsoonStartMonthDay()))
                             .orElse(DEFAULT_MONSOON_START);
        MonthDay mEnd   = cfg.map(c -> mmddToMonthDay(c.getMonsoonEndMonthDay()))
                             .orElse(DEFAULT_MONSOON_END);

        List<MonsoonWarning> warnings = new ArrayList<>();
        for (Task t : tasks.findAllByProjectIdAndMonsoonSensitiveTrue(projectId)) {
            LocalDate start = t.getStartDate();
            LocalDate end   = t.getEndDate();
            if (start == null || end == null) continue;

            for (int year = start.getYear(); year <= end.getYear(); year++) {
                LocalDate mWindowStart = mStart.atYear(year);
                LocalDate mWindowEnd   = mEnd.atYear(year);
                if (overlaps(start, end, mWindowStart, mWindowEnd)) {
                    String severity = (start.compareTo(mWindowStart) >= 0
                            && end.compareTo(mWindowEnd) <= 0)
                            ? "OVERLAP_FULL" : "OVERLAP_PARTIAL";
                    warnings.add(new MonsoonWarning(t.getId(), t.getTitle(),
                            start, end, mWindowStart, mWindowEnd, severity));
                    break; // collapse multi-year overlaps to one warning per task
                }
            }
        }
        return warnings;
    }

    private static boolean overlaps(LocalDate aStart, LocalDate aEnd,
                                    LocalDate bStart, LocalDate bEnd) {
        return !aStart.isAfter(bEnd) && !aEnd.isBefore(bStart);
    }

    private static MonthDay mmddToMonthDay(Short mmdd) {
        int v = mmdd != null ? mmdd : 601;
        return MonthDay.of(v / 100, v % 100);
    }
}
